package com.mana.openhand_backend.identity.businesslayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfileRequest;
import com.mana.openhand_backend.identity.dataaccesslayer.Gender;

@ExtendWith(MockitoExtension.class)
class UserMemberServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserMemberServiceImpl service;

    @Test
    void deactivateAccount_setsInactive_andRevokesTokens() {
        // arrange
        User user = new User();
        user.setId(42L);
        user.setMemberStatus(MemberStatus.ACTIVE);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenAnswer(inv -> {
            user.setStatusChangedAt(LocalDateTime.now());
            return user;
        });

        // act
        User result = service.deactivateAccount(42L);

        // assert
        assertThat(result.getMemberStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(result.getStatusChangedAt()).isNotNull();
        verify(refreshTokenService).deleteByUserId(42L);
    }

    @Test
    void deactivateAccount_whenAlreadyInactive_doesNotRevokeAgain() {
        // arrange
        User user = new User();
        user.setId(99L);
        user.setMemberStatus(MemberStatus.INACTIVE);
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));

        // act
        User result = service.deactivateAccount(99L);

        // assert
        assertThat(result.getMemberStatus()).isEqualTo(MemberStatus.INACTIVE);
        verify(refreshTokenService, never()).deleteByUserId(anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateAccount_whenUserMissing_throws() {
        // arrange
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        // act & assert
        org.junit.jupiter.api.Assertions.assertThrows(UserNotFoundException.class,
                () -> service.deactivateAccount(7L));
    }

    @Test
    void getProfile_whenUserExists_returnsUser() {
        User user = new User();
        user.setId(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));

        User result = service.getProfile(11L);

        assertThat(result.getId()).isEqualTo(11L);
    }

    @Test
    void getProfile_whenMissing_throws() {
        when(userRepository.findById(12L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(UserNotFoundException.class,
                () -> service.getProfile(12L));
    }

    @Test
    void updateProfile_updatesProvidedFields() {
        User user = new User();
        user.setId(21L);
        user.setName("Old Name");
        user.setPhoneNumber("123");
        user.setPreferredLanguage("en");
        user.setGender(Gender.MALE);
        user.setAge(20);

        when(userRepository.findById(21L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileRequest request = new ProfileRequest(
                "New Name",
                "+14155551234",
                "fr",
                "FEMALE",
                35
        );

        User updated = service.updateProfile(21L, request);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getPhoneNumber()).isEqualTo("+14155551234");
        assertThat(updated.getPreferredLanguage()).isEqualTo("fr");
        assertThat(updated.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(updated.getAge()).isEqualTo(35);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_ignoresInvalidAgeAndNulls() {
        User user = new User();
        user.setId(22L);
        user.setName("Keep Name");
        user.setAge(25);

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileRequest request = new ProfileRequest(
                null,
                null,
                null,
                null,
                10
        );

        User updated = service.updateProfile(22L, request);

        assertThat(updated.getName()).isEqualTo("Keep Name");
        assertThat(updated.getAge()).isEqualTo(25);
    }

    @Test
    void updateProfile_withInvalidGender_throws() {
        User user = new User();
        user.setId(23L);
        when(userRepository.findById(23L)).thenReturn(Optional.of(user));

        ProfileRequest request = new ProfileRequest(
                null,
                null,
                null,
                "INVALID",
                null
        );

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(23L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_whenUserMissing_throws() {
        when(userRepository.findById(24L)).thenReturn(Optional.empty());

        ProfileRequest request = new ProfileRequest(
                "Name",
                null,
                null,
                null,
                null
        );

        org.junit.jupiter.api.Assertions.assertThrows(UserNotFoundException.class,
                () -> service.updateProfile(24L, request));
    }
}
