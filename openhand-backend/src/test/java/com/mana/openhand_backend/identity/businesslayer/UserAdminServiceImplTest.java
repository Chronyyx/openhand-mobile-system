package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserAdminServiceImpl userAdminService;

    @BeforeEach
    void setUp() {
        userAdminService = new UserAdminServiceImpl(userRepository);
    }

    // ---------------- getAllUsers ----------------

    @Test
    void getAllUsers_returnsUsersSortedByEmailAsc() {
        List<User> users = List.of(mock(User.class));
        when(userRepository.findAll(any(Sort.class))).thenReturn(users);

        List<User> result = userAdminService.getAllUsers();

        assertSame(users, result);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(userRepository).findAll(sortCaptor.capture());

        Sort expectedSort = Sort.by(Sort.Direction.ASC, "email");
        assertEquals(expectedSort, sortCaptor.getValue());
    }

    // ---------------- updateUserRoles (happy path) ----------------

    @Test
    void updateUserRoles_validRoles_normalizesAndSaves() {
        Long userId = 1L;
        Set<String> roles = Set.of("admin", "member");
        User user = mock(User.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userAdminService.updateUserRoles(userId, roles);

        Set<String> expectedNormalized = RoleUtils.normalizeRoles(roles);

        verify(userRepository).findById(userId);
        verify(user).setRoles(expectedNormalized);
        verify(userRepository).save(user);
        assertSame(user, result);
    }

    // ---------------- updateUserRoles (invalid roles) ----------------

    @Test
    void updateUserRoles_invalidRoles_throwsInvalidRoleException() {
        Long userId = 1L;
        // empty set -> RoleUtils.normalizeRoles throws IllegalArgumentException
        Set<String> invalidRoles = Collections.emptySet();

        assertThrows(
                InvalidRoleException.class,
                () -> userAdminService.updateUserRoles(userId, invalidRoles)
        );

        // normalizeRoles fails before any repository call
        verifyNoInteractions(userRepository);
    }

    // ---------------- updateUserRoles (user not found) ----------------

    @Test
    void updateUserRoles_userNotFound_throwsUserNotFoundException() {
        Long userId = 99L;
        Set<String> roles = Set.of("member");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userAdminService.updateUserRoles(userId, roles)
        );

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    // ---------------- getAvailableRoles ----------------

    @Test
    void getAvailableRoles_returnsSortedAllowedRoles() {
        List<String> roles = userAdminService.getAvailableRoles();

        List<String> expected = new ArrayList<>(RoleUtils.ALLOWED_ROLES);
        expected.sort(String::compareTo);

        assertEquals(expected, roles);
    }
}
