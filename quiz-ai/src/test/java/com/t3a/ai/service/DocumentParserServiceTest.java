package com.t3a.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档解析服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
class DocumentParserServiceTest {

    @Autowired
    private DocumentParserService documentParserService;

    @Test
    void testParseTxt_Success() throws Exception {
        String content = "这是一段测试文本\n用于测试TXT文件解析功能";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.getBytes()
        );

        String parsed = documentParserService.parseDocument(file);

        assertNotNull(parsed);
        assertTrue(parsed.contains("测试文本"));
        assertTrue(parsed.contains("解析功能"));
    }

    @Test
    void testValidateFileSize_TooLarge() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        assertThrows(IllegalArgumentException.class, () -> {
            documentParserService.validateFileSize(file);
        });
    }

    @Test
    void testValidateFileType_Invalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            documentParserService.validateFileType(file);
        });
    }

    @Test
    void testValidateFileType_Valid() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertDoesNotThrow(() -> {
            documentParserService.validateFileType(pdfFile);
        });

        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        assertDoesNotThrow(() -> {
            documentParserService.validateFileType(txtFile);
        });
    }
}
