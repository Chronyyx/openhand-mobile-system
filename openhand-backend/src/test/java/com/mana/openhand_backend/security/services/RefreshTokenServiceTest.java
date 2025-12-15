package com.mana.openhand_backend.security.services;

import com.mana.openhand_backend.identity.dataaccesslayer.RefreshToken;
import com.mana.openhand_backend.identity.dataaccesslayer.RefreshTokenRepository;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 1000L);
    }

    @Test
    void createRefreshToken_withExistingUser_setsFieldsAndSaves() {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(42L, "agent");

        assertNotNull(token.getToken());
        assertEquals(user, token.getUser());
        assertEquals("agent", token.getUserAgent());
        assertTrue(token.getExpiryDate().isAfter(Instant.now()));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_missingUser_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> refreshTokenService.createRefreshToken(99L, "agent"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyExpiration_withValidToken_returnsToken() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().plusSeconds(60));
        RefreshToken result = refreshTokenService.verifyExpiration(token);
        assertSame(token, result);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_withExpiredToken_deletesAndThrows() {
        RefreshToken token = new RefreshToken();
        token.setToken("old");
        token.setExpiryDate(Instant.now().minusSeconds(10));

        assertThrows(TokenExpiredException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void verifyUserAgent_mismatch_throws() {
        RefreshToken token = new RefreshToken();
        token.setUserAgent("agent-a");
        assertThrows(RuntimeException.class, () -> refreshTokenService.verifyUserAgent(token, "agent-b"));
    }

    @Test
    void verifyUserAgent_matching_allows() {
        RefreshToken token = new RefreshToken();
        token.setUserAgent("agent-a");
        assertDoesNotThrow(() -> refreshTokenService.verifyUserAgent(token, "agent-a"));
    }

    @Test
    void deleteByUserId_withExistingUser_deletes() {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.deleteByUser(user)).thenReturn(3);

        int deleted = refreshTokenService.deleteByUserId(5L);
        assertEquals(3, deleted);
    }

    @Test
    void deleteByUserId_missingUser_throws() {
        when(userRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> refreshTokenService.deleteByUserId(8L));
        verify(refreshTokenRepository, never()).deleteByUser(any());
    }

    @Test
    void rotateRefreshToken_replacesTokenAndKeepsUserAgent() {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(7L);
        RefreshToken old = new RefreshToken();
        old.setUser(user);
        old.setUserAgent("agent-1");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotateRefreshToken(old);

        verify(refreshTokenRepository).delete(old);
        assertEquals(user, rotated.getUser());
        assertEquals("agent-1", rotated.getUserAgent());
        assertNotEquals(old.getToken(), rotated.getToken());
    }
}
