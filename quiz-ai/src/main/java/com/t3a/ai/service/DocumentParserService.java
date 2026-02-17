package com.t3a.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析服务
 */
@Slf4j
@Service
public class DocumentParserService {

    /**
     * 解析上传的文件，提取文本内容
     */
    public String parseDocument(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();

        return switch (extension) {
            case "pdf" -> parsePdf(file.getInputStream());
            case "txt" -> parseTxt(file.getInputStream());
            case "docx", "doc" -> parseWord(file.getInputStream());
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };
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
     * 验证文件大小（最大10MB）
     */
    public void validateFileSize(MultipartFile file) {
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
    }

    /**
     * 验证文件类型
     */
    public void validateFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!extension.matches("pdf|txt|docx?")) {
            throw new IllegalArgumentException("只支持 PDF、TXT、DOC、DOCX 格式");
        }
    }
}
