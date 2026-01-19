package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.EmployeeRegistrationRequestModel;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeRegistrationControllerTest {

    @Mock
    private RegistrationService registrationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private EmployeeRegistrationController employeeRegistrationController;

    private User participantUser;
    private User actorUser;
    private Event testEvent;
    private Registration testRegistration;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        participantUser = new User();
        participantUser.setEmail("participant@example.com");
        participantUser.setId(2L);
    participantUser.setName("John Doe");

        actorUser = new User();
        actorUser.setEmail("employee@example.com");
        actorUser.setId(1L);
        actorUser.setPreferredLanguage("en");

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

        testRegistration = new Registration(participantUser, testEvent);
        testRegistration.setStatus(RegistrationStatus.CONFIRMED);
        testRegistration.setConfirmedAt(now);
    }

    private void setupAuthentication(String email) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(actorUser));
    }

    @Test
    void registerParticipant_withValidRequest_shouldRegisterAndNotifyActor() {
        // Arrange
        setupAuthentication("employee@example.com");
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        when(registrationService.registerForEvent(2L, 1L)).thenReturn(testRegistration);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participantUser));

        // Act
        RegistrationResponseModel response = employeeRegistrationController.registerParticipant(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        verify(registrationService).registerForEvent(2L, 1L);
        verify(userRepository).findByEmail("employee@example.com");
        verify(notificationService).createNotification(1L, 1L, "EMPLOYEE_REGISTERED_PARTICIPANT", "en", "John Doe");
    }

    @Test
    void registerParticipant_withWaitlistedStatus_shouldRegisterAndNotifyActor() {
        // Arrange
        setupAuthentication("employee@example.com");
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        Registration waitlistedReg = new Registration(participantUser, testEvent);
        waitlistedReg.setStatus(RegistrationStatus.WAITLISTED);
        waitlistedReg.setWaitlistedPosition(1);

        when(registrationService.registerForEvent(2L, 1L)).thenReturn(waitlistedReg);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participantUser));

        // Act
        RegistrationResponseModel response = employeeRegistrationController.registerParticipant(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("WAITLISTED", response.getStatus());
        verify(registrationService).registerForEvent(2L, 1L);
        verify(notificationService).createNotification(1L, 1L, "EMPLOYEE_REGISTERED_PARTICIPANT", "en", "John Doe");
    }

    @Test
    void registerParticipant_whenNotificationFails_shouldNotBreakRegistration() {
        // Arrange
        setupAuthentication("employee@example.com");
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        when(registrationService.registerForEvent(2L, 1L)).thenReturn(testRegistration);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participantUser));
        doThrow(new RuntimeException("Notification service error"))
                .when(notificationService).createNotification(anyLong(), anyLong(), anyString(), anyString(), anyString());

        // Act
        RegistrationResponseModel response = employeeRegistrationController.registerParticipant(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        verify(notificationService).createNotification(1L, 1L, "EMPLOYEE_REGISTERED_PARTICIPANT", "en", "John Doe");
    }

    @Test
    void registerParticipant_withActorDefaultLanguage_shouldUseEnglish() {
        // Arrange
        actorUser.setPreferredLanguage(null);
        setupAuthentication("employee@example.com");
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        when(registrationService.registerForEvent(2L, 1L)).thenReturn(testRegistration);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participantUser));

        // Act
        RegistrationResponseModel response = employeeRegistrationController.registerParticipant(request, authentication);

        // Assert
        assertNotNull(response);
        verify(notificationService).createNotification(1L, 1L, "EMPLOYEE_REGISTERED_PARTICIPANT", "en", "John Doe");
    }

    @Test
    void registerParticipant_withActorPreferredLanguage_shouldUsePreferredLanguage() {
        // Arrange
        actorUser.setPreferredLanguage("fr");
        setupAuthentication("employee@example.com");
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        when(registrationService.registerForEvent(2L, 1L)).thenReturn(testRegistration);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participantUser));

        // Act
        RegistrationResponseModel response = employeeRegistrationController.registerParticipant(request, authentication);

        // Assert
        assertNotNull(response);
        verify(notificationService).createNotification(1L, 1L, "EMPLOYEE_REGISTERED_PARTICIPANT", "fr", "John Doe");
    }

    @Test
    void registerParticipant_withInvalidActor_shouldThrowException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        when(registrationService.registerForEvent(2L, 1L)).thenReturn(testRegistration);

        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        // Act & Assert
        RegistrationResponseModel response = employeeRegistrationController.registerParticipant(request, authentication);
        
        assertNotNull(response);
        verify(registrationService).registerForEvent(2L, 1L);
    }

    @Test
    void registerParticipant_whenUnexpectedRuntimeException_shouldPropagate() {
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(2L, 1L);

        when(registrationService.registerForEvent(2L, 1L))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class,
                () -> employeeRegistrationController.registerParticipant(request, authentication));
        verifyNoInteractions(notificationService);
    }
}
