package com.mana.openhand_backend.security.services;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetails() {
        // arrange
        String email = "user@example.com";
        User user = new User(email, "hashed-password", Set.of("ROLE_MEMBER"));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // act
        UserDetails result = userDetailsService.loadUserByUsername(email);

        // assert
        assertNotNull(result);
        assertTrue(result instanceof UserDetailsImpl);
        assertEquals(email, result.getUsername());
        verify(userRepository, times(1)).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExist_throwsException() {
        // arrange
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email));

        verify(userRepository, times(1)).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void increaseFailedAttempts_belowThreshold_incrementsAndDoesNotLock() {
        // arrange
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setFailedAttempt(5);
        user.setAccountNonLocked(true);

        // act
        userDetailsService.increaseFailedAttempts(user);

        // assert
        assertEquals(6, user.getFailedAttempt());
        assertTrue(user.isAccountNonLocked());
        verify(userRepository, times(1)).save(user);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void increaseFailedAttempts_reachesThreshold_locksAccount() {
        // arrange
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setFailedAttempt(19);
        user.setAccountNonLocked(true);

        // act
        userDetailsService.increaseFailedAttempts(user);

        // assert
        assertEquals(20, user.getFailedAttempt());
        assertFalse(user.isAccountNonLocked());
        verify(userRepository, times(1)).save(user);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void resetFailedAttempts_whenUserExistsAndHasFailures_resetsAndSaves() {
        // arrange
        String email = "user@example.com";
        User user = new User(email, "pwd", Set.of("ROLE_MEMBER"));
        user.setFailedAttempt(3);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // act
        userDetailsService.resetFailedAttempts(email);

        // assert
        assertEquals(0, user.getFailedAttempt());
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(user);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void resetFailedAttempts_whenUserNotFound_doesNothing() {
        // arrange
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // act
        userDetailsService.resetFailedAttempts(email);

        // assert
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void resetFailedAttempts_whenUserHasNoFailures_doesNotSave() {
        // arrange
        String email = "user@example.com";
        User user = new User(email, "pwd", Set.of("ROLE_MEMBER"));
        user.setFailedAttempt(0);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // act
        userDetailsService.resetFailedAttempts(email);

        // assert
        assertEquals(0, user.getFailedAttempt());
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }
}
