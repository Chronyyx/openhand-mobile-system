package com.mana.openhand_backend.common.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
    }

    @Test
    void validateImageFile_validFile_doesNotThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[100]);
        assertDoesNotThrow(() -> fileStorageService.validateImageFile(file, 1000));
    }

    @Test
    void validateImageFile_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.validateImageFile(file, 1000));
    }

    @Test
    void validateImageFile_tooLarge_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[100]);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.validateImageFile(file, 50));
    }

    @Test
    void validateImageFile_invalidContentType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[10]);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.validateImageFile(file, 1000));
    }

    @Test
    void resolveExtension_fromFilename_returnsExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/jpeg", new byte[10]);
        assertEquals(".png", fileStorageService.resolveExtension(file));
    }

    @Test
    void resolveExtension_noExtensionInName_usesContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image", "image/png", new byte[10]);
        assertEquals(".png", fileStorageService.resolveExtension(file));
    }

    @Test
    void resolveExtension_unknownContentType_defaultsToJpg() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image", "application/octet-stream", new byte[10]);
        assertEquals(".jpg", fileStorageService.resolveExtension(file));
    }

    @Test
    void toPublicUrl_concatenatesCorrectly() {
        assertEquals("https://api.example.com/images/1.jpg",
                fileStorageService.toPublicUrl("https://api.example.com/", "images/1.jpg"));
    }

    @Test
    void toPublicUrl_handlesMissingSlash() {
        assertEquals("https://api.example.com/images/1.jpg",
                fileStorageService.toPublicUrl("https://api.example.com", "images/1.jpg"));
    }

    @Test
    void toPublicUrl_alreadyAbsolute_returnsAsIs() {
        assertEquals("https://cdn.other.com/1.jpg",
                fileStorageService.toPublicUrl("https://api.example.com", "https://cdn.other.com/1.jpg"));
    }
}
