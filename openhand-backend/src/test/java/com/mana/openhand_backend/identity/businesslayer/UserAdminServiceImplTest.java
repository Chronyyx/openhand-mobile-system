package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private RefreshTokenService refreshTokenService;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private UserAdminServiceImpl userAdminService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_returnsSortedList() {
        when(userRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(new User("a@test.com", "p", Set.of("ROLE_MEMBER"))));

        List<User> result = userAdminService.getAllUsers();

        assertEquals(1, result.size());
        verify(userRepository).findAll(any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void updateUserRoles_whenInvalidRole_throwsInvalidRoleException() {
        assertThrows(InvalidRoleException.class, () -> userAdminService.updateUserRoles(1L, Set.of("invalid")));
    }

    @Test
    void updateUserRoles_whenUserMissing_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userAdminService.updateUserRoles(99L, Set.of("member")));
    }

    @Test
    void updateUserRoles_updatesRolesAndAudits() {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "pwd"));

        User updated = userAdminService.updateUserRoles(10L, Set.of("admin"));

        assertTrue(updated.getRoles().contains("ROLE_ADMIN"));
        assertFalse(updated.getRoles().contains("ROLE_MEMBER"));

        ArgumentCaptor<String> previousRoleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newRoleCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditLogService).logRoleChange(
                eq(10L),
                eq("user@example.com"),
                previousRoleCaptor.capture(),
                newRoleCaptor.capture(),
                eq("admin@example.com"),
                eq("127.0.0.1"),
                eq("JUnit"),
                eq("ADMIN_CONSOLE")
        );

        assertTrue(previousRoleCaptor.getValue().contains("ROLE_MEMBER"));
        assertEquals("ROLE_ADMIN", newRoleCaptor.getValue());
    }

    @Test
    void getAvailableRoles_returnsSortedRoles() {
        List<String> roles = userAdminService.getAvailableRoles();

        assertEquals(List.of("ROLE_ADMIN", "ROLE_EMPLOYEE", "ROLE_MEMBER"), roles);
    }

    @Test
    void updateUserStatus_whenInactive_revokesTokensAndCancelsRegistrations() {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(55L);
        user.setMemberStatus(com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.ACTIVE);

        when(userRepository.findById(55L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userAdminService.updateUserStatus(55L,
                com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.INACTIVE);

        assertEquals(com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.INACTIVE, updated.getMemberStatus());
        assertNotNull(updated.getStatusChangedAt());
        verify(refreshTokenService).deleteByUserId(55L);
        verify(registrationService).cancelRegistrationsForUser(55L,
                "Registration cancelled due to account deactivation.");
    }
}
