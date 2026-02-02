package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.Gender;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateUserProfileRequest;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private UserAdminServiceImpl service;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "pass"));
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUserRoles_invalidRole_throwsInvalidRoleException() {
        InvalidRoleException ex = assertThrows(InvalidRoleException.class,
                () -> service.updateUserRoles(1L, Set.of("ROLE_UNKNOWN")));
        assertTrue(ex.getMessage().contains("Unsupported role"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUserRoles_success_logsAudit() {
        User user = new User();
        user.setRoles(Set.of("ROLE_MEMBER"));
        user.setEmail("user@example.com");
        user.setId(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = service.updateUserRoles(5L, Set.of("admin"));

        assertTrue(updated.getRoles().contains("ROLE_ADMIN"));
        verify(auditLogService).logRoleChange(eq(5L), eq("user@example.com"),
                eq("ROLE_MEMBER"), eq("ROLE_ADMIN"), eq("admin@example.com"),
                eq("127.0.0.1"), eq("JUnit"), eq("ADMIN_CONSOLE"));
    }

    @Test
    void updateUserProfile_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        assertThrows(UserNotFoundException.class, () -> service.updateUserProfile(1L, request));
    }

    @Test
    void updateUserProfile_blankEmail_throws() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserProfile(1L, request));
        assertEquals("Email cannot be blank", ex.getMessage());
    }

    @Test
    void updateUserProfile_duplicateEmail_throws() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User other = new User();
        other.setId(2L);
        when(userRepository.findByEmail("duplicate@example.com")).thenReturn(Optional.of(other));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail("duplicate@example.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserProfile(1L, request));
        assertEquals("Email is already in use", ex.getMessage());
    }

    @Test
    void updateUserProfile_duplicatePhone_throws() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User other = new User();
        other.setId(2L);
        when(userRepository.findByPhoneNumber("1234567")).thenReturn(Optional.of(other));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPhoneNumber("123-4567");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserProfile(1L, request));
        assertEquals("Phone number is already in use", ex.getMessage());
    }

    @Test
    void updateUserProfile_invalidGender_throws() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setGender("INVALID");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserProfile(1L, request));
        assertTrue(ex.getMessage().contains("Invalid gender value"));
    }

    @Test
    void updateUserProfile_phoneBlank_setsNullAndSaves() {
        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("555");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPhoneNumber("   ");

        User updated = service.updateUserProfile(1L, request);
        assertNull(updated.getPhoneNumber());
    }

    @Test
    void updateUserProfile_success_updatesFields() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail(" test@example.com ");
        request.setName(" Test User ");
        request.setPhoneNumber("+1 (555) 123");
        request.setGender(Gender.MALE.name());
        request.setAge(30);

        User updated = service.updateUserProfile(1L, request);

        assertEquals("test@example.com", updated.getEmail());
        assertEquals("Test User", updated.getName());
        assertEquals("+1555123", updated.getPhoneNumber());
        assertEquals(Gender.MALE, updated.getGender());
        assertEquals(30, updated.getAge());
    }

    @Test
    void updateUserStatus_nullStatus_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUserStatus(1L, null));
    }

    @Test
    void updateUserStatus_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.updateUserStatus(1L, MemberStatus.ACTIVE));
    }

    @Test
    void updateUserStatus_sameInactive_cancelsAndNoSave() {
        User user = new User();
        user.setId(1L);
        user.setMemberStatus(MemberStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User updated = service.updateUserStatus(1L, MemberStatus.INACTIVE);

        assertSame(user, updated);
        verify(refreshTokenService).deleteByUserId(1L);
        verify(registrationService).cancelRegistrationsForUser(eq(1L), any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserStatus_sameActive_noSideEffects() {
        User user = new User();
        user.setId(1L);
        user.setMemberStatus(MemberStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User updated = service.updateUserStatus(1L, MemberStatus.ACTIVE);

        assertSame(user, updated);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(registrationService);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserStatus_changeToInactive_savesAndCancels() {
        User user = new User();
        user.setId(1L);
        user.setMemberStatus(MemberStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = service.updateUserStatus(1L, MemberStatus.INACTIVE);

        assertEquals(MemberStatus.INACTIVE, updated.getMemberStatus());
        assertNotNull(updated.getStatusChangedAt());
        verify(refreshTokenService).deleteByUserId(1L);
        verify(registrationService).cancelRegistrationsForUser(eq(1L), any(String.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserStatus_changeToActive_savesOnly() {
        User user = new User();
        user.setId(1L);
        user.setMemberStatus(MemberStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = service.updateUserStatus(1L, MemberStatus.ACTIVE);

        assertEquals(MemberStatus.ACTIVE, updated.getMemberStatus());
        assertNotNull(updated.getStatusChangedAt());
        verify(refreshTokenService, never()).deleteByUserId(any(Long.class));
        verify(registrationService, never()).cancelRegistrationsForUser(any(Long.class), any(String.class));
        verify(userRepository).save(any(User.class));
    }
}
