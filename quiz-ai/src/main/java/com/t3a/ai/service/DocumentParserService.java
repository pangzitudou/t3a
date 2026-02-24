package com.t3a.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文档解析服务
 */
@Slf4j
@Service
public class DocumentParserService {

    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private DataSize maxFileSize;

    /**
     * 解析上传的文件，提取文本内容
     */
    public String parseDocument(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) throw new IllegalArgumentException("文件名不能为空");
        return parseDocument(originalFilename, file.getBytes());
    }

    /**
     * 解析上传的文件字节，提取文本内容（避免异步场景下临时文件失效）
     */
    public String parseDocument(String originalFilename, byte[] fileBytes) throws IOException {
        validateFileType(originalFilename);
        String extension = getFileExtension(originalFilename).toLowerCase();
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return switch (extension) {
                case "pdf" -> parsePdf(inputStream);
                case "txt" -> parseTxt(inputStream);
                case "docx", "doc" -> parseWord(inputStream);
                case "epub" -> parseEpub(inputStream);
                default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
            };
        }
    }

    /**
     * 解析 PDF 文件
     */
    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF解析完成，提取文本长度: {}", text.length());
            return text;
        }
    }

    /**
     * 解析 TXT 文件
     */
    private String parseTxt(InputStream inputStream) throws IOException {
        String text = new String(inputStream.readAllBytes());
        log.info("TXT解析完成，文本长度: {}", text.length());
        return text;
    }

    /**
     * 解析 Word 文件
     */
    private String parseWord(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("Word解析完成，文本长度: {}", text.length());
            return text;
        }
    }

    /**
     * 解析 EPUB 文件
     * EPUB 本质是 ZIP 文件，包含 HTML/XHTML 内容文件
     */
    private String parseEpub(InputStream inputStream) throws IOException {
        StringBuilder text = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                // 只解析 HTML/XHTML 文件，跳过 CSS、图片、字体等
                if (name.endsWith(".html") || name.endsWith(".xhtml") ||
                    name.endsWith(".htm")) {
                    ByteArrayOutputStream content = new ByteArrayOutputStream();
                    while ((len = zis.read(buffer)) > 0) {
                        content.write(buffer, 0, len);
                    }
                    String html = content.toString("UTF-8");
                    String chapterText = Jsoup.parse(html).text();
                    if (!chapterText.trim().isEmpty()) {
                        text.append(chapterText).append("\n\n");
                    }
                }
                zis.closeEntry();
            }
        }
        log.info("EPUB解析完成，文本长度: {}", text.length());
        return text.toString();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 验证文件大小（与 multipart max-file-size 配置保持一致）
     */
    public void validateFileSize(MultipartFile file) {
        validateFileSize(file.getSize());
    }

    public void validateFileSize(long fileSize) {
        long maxSize = maxFileSize.toBytes();
        if (fileSize > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过" + maxFileSize);
        }
    }

    /**
     * 验证文件类型
     */
    public void validateFileType(MultipartFile file) {
        validateFileType(file.getOriginalFilename());
    }

    public void validateFileType(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!extension.matches("pdf|txt|docx?|epub")) {
            throw new IllegalArgumentException("只支持 PDF、TXT、EPUB、DOC、DOCX 格式");
        }
    }
}
