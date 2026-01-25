package com.mana.openhand_backend.identity.businesslayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
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
    }

    @Test
    void deactivateAccount_whenUserMissing_throws() {
        // arrange
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        // act & assert
        org.junit.jupiter.api.Assertions.assertThrows(UserNotFoundException.class,
                () -> service.deactivateAccount(7L));
    }
}
