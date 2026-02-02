package com.mana.openhand_backend.common.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    private final FileStorageService service = new FileStorageService();

    @Test
    void validateImageFile_rejectsNullOrEmpty() {
        IllegalArgumentException nullEx = assertThrows(IllegalArgumentException.class,
                () -> service.validateImageFile(null, 10));
        assertEquals("No file provided.", nullEx.getMessage());

        MockMultipartFile empty = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        IllegalArgumentException emptyEx = assertThrows(IllegalArgumentException.class,
                () -> service.validateImageFile(empty, 10));
        assertEquals("No file provided.", emptyEx.getMessage());
    }

    @Test
    void validateImageFile_rejectsSizeAndContentType() {
        MockMultipartFile big = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[20]);
        IllegalArgumentException sizeEx = assertThrows(IllegalArgumentException.class,
                () -> service.validateImageFile(big, 10));
        assertTrue(sizeEx.getMessage().contains("File is too large"));

        MockMultipartFile badType = new MockMultipartFile("file", "test.gif", "image/gif", new byte[1]);
        IllegalArgumentException typeEx = assertThrows(IllegalArgumentException.class,
                () -> service.validateImageFile(badType, 10));
        assertEquals("Only JPEG, PNG, or WEBP images are allowed.", typeEx.getMessage());
    }

    @Test
    void validateImageFile_acceptsValid() {
        MockMultipartFile ok = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[] { 1, 2 });
        assertDoesNotThrow(() -> service.validateImageFile(ok, 10));
    }

    @Test
    void resolveExtension_prefersFilenameThenContentType() {
        MultipartFile fileWithExt = Mockito.mock(MultipartFile.class);
        when(fileWithExt.getOriginalFilename()).thenReturn("photo.PNG");
        when(fileWithExt.getContentType()).thenReturn("image/png");
        assertEquals(".png", service.resolveExtension(fileWithExt));

        MultipartFile fileWithLongExt = Mockito.mock(MultipartFile.class);
        when(fileWithLongExt.getOriginalFilename()).thenReturn("photo.longextension");
        when(fileWithLongExt.getContentType()).thenReturn("image/webp");
        assertEquals(".webp", service.resolveExtension(fileWithLongExt));

        MultipartFile fileUnknown = Mockito.mock(MultipartFile.class);
        when(fileUnknown.getOriginalFilename()).thenReturn("photo");
        when(fileUnknown.getContentType()).thenReturn("application/octet-stream");
        assertEquals(".jpg", service.resolveExtension(fileUnknown));
    }

    @Test
    void storeFile_successWritesToDisk(@TempDir Path tempDir) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        String name = service.storeFile(file, tempDir, "my file");
        assertNotNull(name);
        Path stored = tempDir.resolve(name);
        assertTrue(Files.exists(stored));
    }

    @Test
    void storeFile_rejectsInvalidFilenameBase(@TempDir Path tempDir) {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, tempDir, null));
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, tempDir, "   "));
    }

    @Test
    void storeFile_wrapsIoException(@TempDir Path tempDir) throws Exception {
        MultipartFile badFile = Mockito.mock(MultipartFile.class);
        when(badFile.getOriginalFilename()).thenReturn("bad.jpg");
        when(badFile.getContentType()).thenReturn("image/jpeg");
        when(badFile.getInputStream()).thenThrow(new IOException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.storeFile(badFile, tempDir, "safe"));
        assertEquals("Unable to store file. Please try again.", ex.getMessage());
    }

    @Test
    void cleanupFile_deletesExistingFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("temp.txt");
        Files.writeString(file, "data");
        assertTrue(Files.exists(file));
        service.cleanupFile(file);
        assertFalse(Files.exists(file));

        assertDoesNotThrow(() -> service.cleanupFile(null));
    }

    @Test
    void deleteFile_delegatesToCleanup(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("temp.txt");
        Files.writeString(file, "data");
        service.deleteFile(file);
        assertFalse(Files.exists(file));
    }

    @Test
    void toPublicUrl_handlesNullsAndFullUrls() {
        assertNull(service.toPublicUrl("https://example.com", null));
        assertNull(service.toPublicUrl("https://example.com", " "));
        assertEquals("https://cdn.example.com/a.png",
                service.toPublicUrl("https://example.com", "https://cdn.example.com/a.png"));
        assertEquals("https://example.com/uploads/a.png",
                service.toPublicUrl("https://example.com/", "uploads/a.png"));
        assertEquals("/uploads/a.png", service.toPublicUrl(null, "/uploads/a.png"));
    }
}
