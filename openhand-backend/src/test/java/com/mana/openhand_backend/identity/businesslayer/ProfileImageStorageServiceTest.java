package com.mana.openhand_backend.identity.businesslayer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeProfileImage_withNullFile_throws() {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.storeProfileImage(null));

        assertEquals("File is required.", ex.getMessage());
    }

    @Test
    void storeProfileImage_withEmptyFile_throws() {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.storeProfileImage(emptyFile));

        assertEquals("File is required.", ex.getMessage());
    }

    @Test
    void storeProfileImage_withInvalidContentType_throws() {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                new byte[10]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.storeProfileImage(file));

        assertEquals("Invalid image type.", ex.getMessage());
    }

    @Test
    void storeProfileImage_withValidFile_persistsAndReturnsUrl() throws IOException {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3});

        ProfileImageStorageService.StoredProfileImage stored = service.storeProfileImage(file);

        assertNotNull(stored);
        assertTrue(stored.url().startsWith("/uploads/profile-pictures/"));
        Path storedPath = tempDir.resolve(stored.filename());
        assertTrue(Files.exists(storedPath));
    }

    @Test
    void deleteByUrlIfPresent_ignoresBlankAndUnknownPrefix() throws IOException {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        service.deleteByUrlIfPresent(null);
        service.deleteByUrlIfPresent("   ");
        service.deleteByUrlIfPresent("/other/path/image.png");

        Path untouched = tempDir.resolve("keep.png");
        Files.write(untouched, new byte[]{1});
        assertTrue(Files.exists(untouched));
    }

    @Test
    void deleteByUrlIfPresent_deletesMatchingFile() throws IOException {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        Path stored = tempDir.resolve("delete.png");
        Files.write(stored, new byte[]{1});

        service.deleteByUrlIfPresent("/uploads/profile-pictures/delete.png");

        assertFalse(Files.exists(stored));
    }

    @Test
    void deleteByUrlIfPresent_doesNotDeleteWhenPathTraversalDetected() throws IOException {
        ProfileImageStorageService service = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures",
                1024);

        Path parentFile = tempDir.getParent().resolve("evil.png");
        Files.write(parentFile, new byte[]{1});

        service.deleteByUrlIfPresent("/uploads/profile-pictures/../evil.png");

        assertTrue(Files.exists(parentFile));
    }

    @Test
    void urlPathNormalization_andMaxSizeAccessors() {
        ProfileImageStorageService withDefaultPath = new ProfileImageStorageService(
                tempDir.toString(),
                null,
                2048);
        ProfileImageStorageService withTrailingSlash = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-pictures/",
                4096);

        assertEquals("/uploads/profile-pictures", withDefaultPath.getUrlPathPrefix());
        assertEquals("/uploads/profile-pictures", withTrailingSlash.getUrlPathPrefix());
        assertEquals(2048, withDefaultPath.getMaxSizeBytes());
        assertEquals(4096, withTrailingSlash.getMaxSizeBytes());
    }
}
