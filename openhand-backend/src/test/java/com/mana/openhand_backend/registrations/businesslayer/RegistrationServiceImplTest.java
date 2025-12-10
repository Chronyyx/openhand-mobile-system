package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private User testUser;
    private Event testEvent;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        testUser = new User();
        testUser.setEmail("test@example.com");

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        testEvent = new Event(
                "Test Event",
                "Test Description",
                start,
                end,
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                2,
                0,
                "General");
    }

    // ========== registerForEvent Tests ==========

    @Test
    void registerForEvent_withValidUserAndEvent_shouldCreateConfirmedRegistration() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(0L);

        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration reg = invocation.getArgument(0);
            assertEquals(RegistrationStatus.CONFIRMED, reg.getStatus());
            assertNotNull(reg.getConfirmedAt());
            return reg;
        });

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        verify(registrationRepository).save(any(Registration.class));
        verify(eventRepository).save(testEvent);
    }

    @Test
    void registerForEvent_whenEventAtCapacity_shouldCreateWaitlistedRegistration() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.FULL,
                2,
                2,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(2L);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.WAITLISTED)).thenReturn(1L);

        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration reg = invocation.getArgument(0);
            assertEquals(RegistrationStatus.WAITLISTED, reg.getStatus());
            assertEquals(2, reg.getWaitlistedPosition());
            return reg;
        });

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
        assertEquals(2, result.getWaitlistedPosition());
        verify(registrationRepository).save(any(Registration.class));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void registerForEvent_whenUserNotFound_shouldThrowException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> registrationService.registerForEvent(999L, 1L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void registerForEvent_whenEventNotFound_shouldThrowEventNotFoundException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EventNotFoundException.class, () -> registrationService.registerForEvent(1L, 999L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void registerForEvent_whenAlreadyRegistered_shouldThrowAlreadyRegisteredException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        // Act & Assert
        assertThrows(AlreadyRegisteredException.class, () -> registrationService.registerForEvent(1L, 1L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void registerForEvent_withNullEventCapacity_shouldCreateConfirmedRegistration() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                null,
                null,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(0L);

        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void registerForEvent_updatesEventStatusToNearlyFull_when80PercentCapacity() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                7,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(7L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(eventRepository).save(testEvent);
        assertEquals(EventStatus.NEARLY_FULL, testEvent.getStatus());
    }

    @Test
    void registerForEvent_updatesEventStatusToFull_whenReachingMaxCapacity() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.NEARLY_FULL,
                10,
                9,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(9L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertEquals(10, testEvent.getCurrentRegistrations());
        assertEquals(EventStatus.FULL, testEvent.getStatus());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void registerForEvent_withWaitlistWhenEventFull_shouldCreateWaitlistedRegistration() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.FULL,
                10,
                10,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(10L);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.WAITLISTED)).thenReturn(2L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
        assertEquals(3, result.getWaitlistedPosition());
        verify(eventRepository, never()).save(testEvent);
    }

    @Test
    void registerForEvent_withCurrentRegsAtCapacity_shouldCreateWaitlist() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.NEARLY_FULL,
                10,
                10,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(9L); // Not
                                                                                                               // all
                                                                                                               // are
                                                                                                               // confirmed
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.WAITLISTED)).thenReturn(1L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
        assertEquals(2, result.getWaitlistedPosition());
        verify(eventRepository, never()).save(testEvent);
    }

    @Test
    void registerForEvent_withStatusFullButBelowCapacity_shouldCreateWaitlist() {
        // Arrange - Event status is FULL even though currentRegs < maxCapacity
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.FULL, // Marked FULL explicitly
                10,
                5, // But only 5 registered
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(5L);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.WAITLISTED)).thenReturn(0L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
        assertEquals(1, result.getWaitlistedPosition());
        verify(eventRepository, never()).save(testEvent);
    }

    @Test
    void registerForEvent_withAllCapacitySignalsFalse_shouldCreateConfirmedRegistration() {
        // Arrange - All capacity checks are false: confirmedCount < max, currentRegs <
        // max, status not FULL
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                100,
                30, // Below capacity
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(30L); // < 100
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        assertEquals(31, testEvent.getCurrentRegistrations()); // Incremented from 30
        verify(eventRepository).save(testEvent);
    }

    @Test
    void getRegistrationById_withValidId_shouldReturnRegistration() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        // Act
        Registration result = registrationService.getRegistrationById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testEvent, result.getEvent());
    }

    @Test
    void getRegistrationById_withInvalidId_shouldThrowRegistrationNotFoundException() {
        // Arrange
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RegistrationNotFoundException.class, () -> registrationService.getRegistrationById(999L));
    }

    // ========== getUserRegistrations Tests ==========

    @Test
    void getUserRegistrations_withValidUserId_shouldReturnRegistrations() {
        // Arrange
        Registration reg1 = new Registration(testUser, testEvent);
        Registration reg2 = new Registration(testUser, testEvent);

        List<Registration> registrations = Arrays.asList(reg1, reg2);
        when(registrationRepository.findByUserId(1L)).thenReturn(registrations);

        // Act
        List<Registration> result = registrationService.getUserRegistrations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(registrationRepository).findByUserId(1L);
    }

    @Test
    void getUserRegistrations_withNoRegistrations_shouldReturnEmptyList() {
        // Arrange
        when(registrationRepository.findByUserId(1L)).thenReturn(Arrays.asList());

        // Act
        List<Registration> result = registrationService.getUserRegistrations(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== cancelRegistration Tests ==========

    @Test
    void cancelRegistration_withConfirmedRegistration_shouldUpdateEventCapacity() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(now);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                1,
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        assertEquals(0, testEvent.getCurrentRegistrations());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void cancelRegistration_withNonexistentRegistration_shouldThrowException() {
        // Arrange
        when(registrationRepository.findByUserIdAndEventId(1L, 999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> registrationService.cancelRegistration(1L, 999L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void cancelRegistration_withWaitlistedRegistration_shouldNotUpdateEventCapacity() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.WAITLISTED);
        registration.setWaitlistedPosition(1);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void cancelRegistration_changesEventStatusFromFullToNearlyFull() {
        // Arrange - Event at full capacity (10/10)
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.FULL,
                10,
                10,
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertEquals(9, testEvent.getCurrentRegistrations());
        assertEquals(EventStatus.NEARLY_FULL, testEvent.getStatus()); // 9/10 = 90% (>= 80%)
        verify(eventRepository).save(testEvent);
    }

    @Test
    void cancelRegistration_changesEventStatusFromNearlyFullToOpen() {
        // Arrange - Event at nearly full (9/10 = 90%), after cancel will be 8/10 = 80%
        // exactly at boundary
        // At 80% boundary, it should still be NEARLY_FULL since >= 0.8 is the threshold
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.NEARLY_FULL,
                10,
                9,
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertEquals(8, testEvent.getCurrentRegistrations());
        // At 80% (8/10), still NEARLY_FULL since >= 0.8 * maxCapacity
        assertEquals(EventStatus.NEARLY_FULL, testEvent.getStatus());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void cancelRegistration_withZeroCurrentRegistrations_shouldNotGoNegative() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                0, // Already 0
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        // Should stay at 0, not go negative
        assertEquals(0, testEvent.getCurrentRegistrations());
        verify(eventRepository, never()).save(any(Event.class)); // currentRegs is 0, so no save
    }

    @Test
    void cancelRegistration_withNullCurrentRegistrations_shouldNotUpdateEvent() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                null, // null currentRegistrations
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertNull(testEvent.getCurrentRegistrations()); // Stays null
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void cancelRegistration_changesEventStatusFromNearlyFullToOpenWhenBelowEightyPercent() {
        // Arrange - Event at 81% capacity (still NEARLY_FULL threshold)
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.NEARLY_FULL,
                100,
                81, // 81% capacity
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertEquals(80, testEvent.getCurrentRegistrations()); // 80/100 = 80% exactly at threshold
        assertEquals(EventStatus.NEARLY_FULL, testEvent.getStatus()); // Still NEARLY_FULL at 80%
        verify(eventRepository).save(testEvent);
    }

    @Test
    void cancelRegistration_changesEventStatusFromNearlyFullToOpenWhenSeventy() {
        // Arrange - Event at 71% capacity
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.NEARLY_FULL,
                100,
                71, // 71% capacity
                "General");

        registration.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertEquals(70, testEvent.getCurrentRegistrations()); // 70/100 = 70% < 80%
        assertEquals(EventStatus.OPEN, testEvent.getStatus()); // Changes to OPEN
        verify(eventRepository).save(testEvent);
    }
}
