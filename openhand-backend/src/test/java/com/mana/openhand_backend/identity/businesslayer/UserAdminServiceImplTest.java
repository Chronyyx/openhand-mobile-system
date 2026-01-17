package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.RoleUtils;

import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private UserAdminServiceImpl userAdminService;

    @BeforeEach
    void setUp() {
        userAdminService = new UserAdminServiceImpl(userRepository, auditLogService, request);

        // Mock Security Context
        SecurityContextHolder.setContext(securityContext);
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
    void updateUserRoles_validRoles_normalizesAndSavesAndLogs() {
        Long userId = 1L;
        Set<String> roles = Set.of("admin", "member");
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>(Set.of("ROLE_MEMBER"))); // Old role

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the
                                                                                                        // same user
                                                                                                        // object

        // Mock Security & Request
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("adminUser");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        User result = userAdminService.updateUserRoles(userId, roles);

        Set<String> expectedNormalized = RoleUtils.normalizeRoles(roles);

        // Verify Repository interactions
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
        assertEquals(expectedNormalized, result.getRoles());

        // Verify Audit Log
        verify(auditLogService).logRoleChange(
                eq(userId),
                eq("test@example.com"),
                eq("ROLE_MEMBER"), // Old
                argThat(s -> s.contains("ROLE_ADMIN") && s.contains("ROLE_MEMBER")), // New (order might vary)
                eq("adminUser"),
                eq("127.0.0.1"),
                eq("JUnit"),
                eq("ADMIN_CONSOLE"));
    }

    // ---------------- updateUserRoles (invalid roles) ----------------

    @Test
    void updateUserRoles_invalidRoles_throwsInvalidRoleException() {
        Long userId = 1L;
        Set<String> invalidRoles = Collections.emptySet();

        assertThrows(
                InvalidRoleException.class,
                () -> userAdminService.updateUserRoles(userId, invalidRoles));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(auditLogService);
    }

    // ---------------- updateUserRoles (user not found) ----------------

    @Test
    void updateUserRoles_userNotFound_throwsUserNotFoundException() {
        Long userId = 99L;
        Set<String> roles = Set.of("member");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userAdminService.updateUserRoles(userId, roles));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
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
