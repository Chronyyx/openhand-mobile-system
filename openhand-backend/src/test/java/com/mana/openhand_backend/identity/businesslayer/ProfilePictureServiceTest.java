package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.common.services.FileStorageService;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfilePictureServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @Test
    void toPublicUrl_handlesNullsAndAbsoluteUrl() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024);

        when(fileStorageService.toPublicUrl("http://localhost:8080", null)).thenReturn(null);
        when(fileStorageService.toPublicUrl("http://localhost:8080", "http://cdn.example.com/pic.jpg"))
                .thenReturn("http://cdn.example.com/pic.jpg");
        when(fileStorageService.toPublicUrl("http://localhost:8080/", "/uploads/profile-pictures/a.png"))
                .thenReturn("http://localhost:8080/uploads/profile-pictures/a.png");
        when(fileStorageService.toPublicUrl("http://localhost:8080", "uploads/profile-pictures/a.png"))
                .thenReturn("http://localhost:8080/uploads/profile-pictures/a.png");

        assertNull(service.toPublicUrl("http://localhost:8080", null));
        assertEquals("http://cdn.example.com/pic.jpg",
                service.toPublicUrl("http://localhost:8080", "http://cdn.example.com/pic.jpg"));
        assertEquals("http://localhost:8080/uploads/profile-pictures/a.png",
                service.toPublicUrl("http://localhost:8080/", "/uploads/profile-pictures/a.png"));
        assertEquals("http://localhost:8080/uploads/profile-pictures/a.png",
                service.toPublicUrl("http://localhost:8080", "uploads/profile-pictures/a.png"));
    }

    @Test
    void getProfilePicture_whenUserMissing_throws() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getProfilePicture(1L, "http://localhost:8080"));
    }

    @Test
    void getProfilePicture_returnsResolvedUrl() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024);
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setProfilePictureUrl("/uploads/profile-pictures/pic.png");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fileStorageService.toPublicUrl("http://localhost:8080", "/uploads/profile-pictures/pic.png"))
                .thenReturn("http://localhost:8080/uploads/profile-pictures/pic.png");

        ProfilePictureResponse response = service.getProfilePicture(2L, "http://localhost:8080");

        assertEquals("http://localhost:8080/uploads/profile-pictures/pic.png", response.getUrl());
    }

    @Test
    void storeProfilePicture_rejectsInvalidContentType() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                "nope".getBytes());
        doThrow(new IllegalArgumentException("Invalid content type")).when(fileStorageService).validateImageFile(file,
                1024);

        assertThrows(IllegalArgumentException.class, () -> service.storeProfilePicture(1L, file, "http://localhost"));
    }

    @Test
    void storeProfilePicture_rejectsTooLargeFile() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 4);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pic.jpg",
                "image/jpeg",
                "12345".getBytes());
        doThrow(new IllegalArgumentException("Too large")).when(fileStorageService).validateImageFile(file, 4);

        assertThrows(IllegalArgumentException.class, () -> service.storeProfilePicture(1L, file, "http://localhost"));
    }

    @Test
    void storeProfilePicture_rejectsEmptyFile() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pic.jpg",
                "image/jpeg",
                new byte[0]);
        doThrow(new IllegalArgumentException("Empty")).when(fileStorageService).validateImageFile(file, 1024);

        assertThrows(IllegalArgumentException.class, () -> service.storeProfilePicture(1L, file, "http://localhost"));
    }

    @Test
    void storeProfilePicture_rejectsNullFile() {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024);
        doThrow(new IllegalArgumentException("Null")).when(fileStorageService).validateImageFile(null, 1024);

        assertThrows(IllegalArgumentException.class, () -> service.storeProfilePicture(1L, null, "http://localhost"));
    }

    @Test
    void storeProfilePicture_usesWebpExtensionWhenMissing() throws IOException {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024 * 1024);

        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile",
                "image/webp",
                "data".getBytes());
        when(fileStorageService.storeFile(eq(file), any(), eq("user-9"))).thenReturn("user-9-uuid.webp");
        when(fileStorageService.toPublicUrl("http://localhost:8080", "/uploads/profile-pictures/user-9-uuid.webp"))
                .thenReturn("http://localhost:8080/uploads/profile-pictures/user-9-uuid.webp");

        ProfilePictureResponse response = service.storeProfilePicture(9L, file, "http://localhost:8080");

        assertNotNull(response.getUrl());
        // assertTrue(response.getUrl().endsWith(".webp")); // The mock determines this
        // now, so testing it is verifying the mock
        assertEquals("http://localhost:8080/uploads/profile-pictures/user-9-uuid.webp", response.getUrl());
    }

    @Test
    void storeProfilePicture_savesFileAndUpdatesUser() throws IOException {
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 1024 * 1024);

        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(5L);
        user.setProfilePictureUrl("/uploads/profile-pictures/old.png");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pic.png",
                "image/png",
                "new".getBytes());

        when(fileStorageService.storeFile(eq(file), any(), eq("user-5"))).thenReturn("user-5-uuid.png");
        when(fileStorageService.toPublicUrl("http://localhost:8080", "/uploads/profile-pictures/user-5-uuid.png"))
                .thenReturn("http://localhost:8080/uploads/profile-pictures/user-5-uuid.png");

        ProfilePictureResponse response = service.storeProfilePicture(5L, file, "http://localhost:8080");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertNotNull(saved.getProfilePictureUrl());
        assertTrue(saved.getProfilePictureUrl().startsWith("/uploads/profile-pictures/"));
        assertEquals("http://localhost:8080/uploads/profile-pictures/user-5-uuid.png", response.getUrl());
        // assertFalse(Files.exists(oldFile)); // Logic moved to FileStorageService or
        // deletePreviousFile method
        verify(fileStorageService).deleteFile(argThat(path -> path.endsWith("old.png")));

        // long fileCount = Files.list(tempDir).count(); // No real file creation with
        // mock
        // assertEquals(1, fileCount);
    }
}
