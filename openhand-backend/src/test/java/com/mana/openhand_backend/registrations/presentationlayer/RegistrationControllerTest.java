package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.businesslayer.UserMemberService;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegistrationService registrationService;

    @Mock
    private UserMemberService userMemberService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private RegistrationController registrationController;

    private User testUser;
    private Event testEvent;
    private Registration testRegistration;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        testUser = new User();
        testUser.setEmail("test@example.com");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID for test", e);
        }

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                0,
                "General");

        testRegistration = new Registration(testUser, testEvent);
        testRegistration.setStatus(RegistrationStatus.CONFIRMED);
        testRegistration.setConfirmedAt(now);
    }

    private void setupAuthentication(String email) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        when(userMemberService.getProfileByEmail(email)).thenReturn(testUser);
    }

    // ========== registerForEvent Tests ==========

    @Test
    void registerForEvent_withValidRequest_shouldCallService() {
        // Arrange
        setupAuthentication("test@example.com");
        RegistrationRequestModel request = new RegistrationRequestModel(1L);

        when(registrationService.registerForEvent(1L, 1L)).thenReturn(testRegistration);

        // Act
        RegistrationResponseModel response = registrationController.registerForEvent(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        verify(registrationService).registerForEvent(1L, 1L);
        verify(userMemberService).getProfileByEmail("test@example.com");
    }

    @Test
    void registerForEvent_withWaitlistedRegistration_shouldReturnWaitlistedStatus() {
        // Arrange
        setupAuthentication("test@example.com");
        RegistrationRequestModel request = new RegistrationRequestModel(1L);

        Registration waitlistedReg = new Registration(testUser, testEvent);
        waitlistedReg.setStatus(RegistrationStatus.WAITLISTED);
        waitlistedReg.setWaitlistedPosition(1);

        when(registrationService.registerForEvent(1L, 1L)).thenReturn(waitlistedReg);

        // Act
        RegistrationResponseModel response = registrationController.registerForEvent(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("WAITLISTED", response.getStatus());
        assertEquals(1, response.getWaitlistedPosition());
    }

    @Test
    void registerForEvent_withInvalidUser_shouldThrowException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("nonexistent@example.com");
        when(userMemberService.getProfileByEmail("nonexistent@example.com"))
                .thenThrow(new RuntimeException("User not found"));

        RegistrationRequestModel request = new RegistrationRequestModel(1L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> registrationController.registerForEvent(request, authentication));
    }

    // ========== getMyRegistrations Tests ==========

    @Test
    void getMyRegistrations_withValidUser_shouldCallService() {
        // Arrange
        setupAuthentication("test@example.com");

        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.CONFIRMED);

        Registration reg2 = new Registration(testUser, testEvent);
        reg2.setStatus(RegistrationStatus.WAITLISTED);
        reg2.setWaitlistedPosition(1);

        List<Registration> userRegistrations = Arrays.asList(reg1, reg2);
        when(registrationService.getUserRegistrations(1L)).thenReturn(userRegistrations);

        // Act
        List<RegistrationResponseModel> response = registrationController.getMyRegistrations(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(registrationService).getUserRegistrations(1L);
    }

    @Test
    void getMyRegistrations_withNoRegistrations_shouldReturnEmptyList() {
        // Arrange
        setupAuthentication("test@example.com");
        when(registrationService.getUserRegistrations(1L)).thenReturn(Arrays.asList());

        // Act
        List<RegistrationResponseModel> response = registrationController.getMyRegistrations(authentication);

        // Assert
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void getMyRegistrations_withInvalidUser_shouldThrowException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("nonexistent@example.com");
        when(userMemberService.getProfileByEmail("nonexistent@example.com"))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> registrationController.getMyRegistrations(authentication));
    }

    // ========== cancelRegistration Tests ==========

    @Test
    void cancelRegistration_withValidEventId_shouldCallService() {
        // Arrange
        setupAuthentication("test@example.com");

        Registration cancelledReg = new Registration(testUser, testEvent);
        cancelledReg.setStatus(RegistrationStatus.CANCELLED);
        cancelledReg.setCancelledAt(now);

        when(registrationService.cancelRegistration(1L, 1L)).thenReturn(cancelledReg);

        // Act
        RegistrationResponseModel response = registrationController.cancelRegistration(1L, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        verify(registrationService).cancelRegistration(1L, 1L);
    }

    @Test
    void cancelRegistration_withInvalidEventId_shouldThrowException() {
        // Arrange
        setupAuthentication("test@example.com");
        when(registrationService.cancelRegistration(1L, 999L))
                .thenThrow(new RuntimeException("Registration not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> registrationController.cancelRegistration(999L, authentication));
    }

    @Test
    void cancelRegistration_withInvalidUser_shouldThrowException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("nonexistent@example.com");
        when(userMemberService.getProfileByEmail("nonexistent@example.com"))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> registrationController.cancelRegistration(1L, authentication));
    }

    // ========== Response Mapping Tests ==========

    @Test
    void registerForEvent_withWaitlistedRegistration_shouldMapToResponseModel() {
        // Arrange
        setupAuthentication("test@example.com");
        RegistrationRequestModel request = new RegistrationRequestModel(1L);

        Registration waitlistedReg = new Registration(testUser, testEvent);
        waitlistedReg.setStatus(RegistrationStatus.WAITLISTED);
        waitlistedReg.setWaitlistedPosition(2);

        when(registrationService.registerForEvent(1L, 1L)).thenReturn(waitlistedReg);

        // Act
        RegistrationResponseModel response = registrationController.registerForEvent(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("WAITLISTED", response.getStatus());
        assertEquals(2, response.getWaitlistedPosition());
    }
}
