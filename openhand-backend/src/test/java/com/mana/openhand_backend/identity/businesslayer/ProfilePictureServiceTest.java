package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfilePictureServiceTest {

    @Mock
    private UserRepository userRepository;

    @TempDir
    Path tempDir;

    @Test
    void toPublicUrl_handlesNullsAndAbsoluteUrl() {
        ProfilePictureService service = new ProfilePictureService(userRepository, tempDir.toString(), 1024);

        assertNull(service.toPublicUrl("http://localhost:8080", null));
        assertEquals("http://cdn.example.com/pic.jpg",
                service.toPublicUrl("http://localhost:8080", "http://cdn.example.com/pic.jpg"));
        assertEquals("http://localhost:8080/uploads/profile-pictures/a.png",
                service.toPublicUrl("http://localhost:8080/", "/uploads/profile-pictures/a.png"));
    }

    @Test
    void getProfilePicture_whenUserMissing_throws() {
        ProfilePictureService service = new ProfilePictureService(userRepository, tempDir.toString(), 1024);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getProfilePicture(1L, "http://localhost:8080"));
    }

    @Test
    void getProfilePicture_returnsResolvedUrl() {
        ProfilePictureService service = new ProfilePictureService(userRepository, tempDir.toString(), 1024);
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setProfilePictureUrl("/uploads/profile-pictures/pic.png");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        ProfilePictureResponse response = service.getProfilePicture(2L, "http://localhost:8080");

        assertEquals("http://localhost:8080/uploads/profile-pictures/pic.png", response.getUrl());
    }

    @Test
    void storeProfilePicture_rejectsInvalidContentType() {
        ProfilePictureService service = new ProfilePictureService(userRepository, tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                "nope".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> service.storeProfilePicture(1L, file, "http://localhost"));
    }

    @Test
    void storeProfilePicture_rejectsTooLargeFile() {
        ProfilePictureService service = new ProfilePictureService(userRepository, tempDir.toString(), 4);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pic.jpg",
                "image/jpeg",
                "12345".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> service.storeProfilePicture(1L, file, "http://localhost"));
    }

    @Test
    void storeProfilePicture_savesFileAndUpdatesUser() throws IOException {
        ProfilePictureService service = new ProfilePictureService(userRepository, tempDir.toString(), 1024 * 1024);

        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(5L);
        user.setProfilePictureUrl("/uploads/profile-pictures/old.png");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        Path oldFile = tempDir.resolve("old.png");
        Files.write(oldFile, "old".getBytes());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pic.png",
                "image/png",
                "new".getBytes()
        );

        ProfilePictureResponse response = service.storeProfilePicture(5L, file, "http://localhost:8080");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertNotNull(saved.getProfilePictureUrl());
        assertTrue(saved.getProfilePictureUrl().startsWith("/uploads/profile-pictures/"));
        assertTrue(response.getUrl().startsWith("http://localhost:8080/uploads/profile-pictures/"));
        assertFalse(Files.exists(oldFile));

        long fileCount = Files.list(tempDir).count();
        assertEquals(1, fileCount);
    }
}
