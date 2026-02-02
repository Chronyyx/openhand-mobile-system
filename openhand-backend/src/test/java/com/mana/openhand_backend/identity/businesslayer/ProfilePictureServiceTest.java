package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.common.presentationlayer.payload.ImageUrlResponse;
import com.mana.openhand_backend.common.services.FileStorageService;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfilePictureServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Test
    void getProfilePicture_userNotFound_throws(@TempDir Path tempDir) {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 10L);

        assertThrows(UserNotFoundException.class, () -> service.getProfilePicture(1L, "http://base"));
    }

    @Test
    void storeProfilePicture_success_deletesPrevious(@TempDir Path tempDir) {
        User user = new User();
        user.setId(5L);
        user.setProfilePictureUrl("/uploads/profile-pictures/old.png");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.storeFile(any(MultipartFile.class), any(Path.class), anyString()))
                .thenReturn("new.png");
        when(fileStorageService.toPublicUrl(anyString(), anyString()))
                .thenReturn("http://base/uploads/profile-pictures/new.png");

        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 10L);

        ImageUrlResponse response = service.storeProfilePicture(5L, mock(MultipartFile.class), "http://base");

        assertEquals("http://base/uploads/profile-pictures/new.png", response.getUrl());
        ArgumentCaptor<Path> deleteCaptor = ArgumentCaptor.forClass(Path.class);
        verify(fileStorageService).deleteFile(deleteCaptor.capture());
        assertTrue(deleteCaptor.getValue().toString().endsWith("old.png"));
    }

    @Test
    void storeProfilePicture_userNotFound_throws(@TempDir Path tempDir) {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 10L);

        assertThrows(UserNotFoundException.class,
                () -> service.storeProfilePicture(1L, mock(MultipartFile.class), "http://base"));
    }

    @Test
    void storeProfilePicture_saveFails_cleansUp(@TempDir Path tempDir) {
        User user = new User();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(fileStorageService.storeFile(any(MultipartFile.class), any(Path.class), anyString()))
                .thenReturn("new.png");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("fail"));

        ProfilePictureService service = new ProfilePictureService(userRepository, fileStorageService,
                tempDir.toString(), 10L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.storeProfilePicture(5L, mock(MultipartFile.class), "http://base"));
        assertEquals("fail", ex.getMessage());
        verify(fileStorageService).deleteFile(any(Path.class));
    }
}
