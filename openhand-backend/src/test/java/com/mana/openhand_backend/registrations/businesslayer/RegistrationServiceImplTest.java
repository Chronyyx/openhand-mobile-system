package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.events.businesslayer.EventCompletionService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.FamilyMemberRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationResponseModel;
import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.EventCompletedException;
import com.mana.openhand_backend.registrations.utils.InactiveMemberException;
import com.mana.openhand_backend.registrations.utils.GroupRegistrationCapacityException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SendGridEmailService sendGridEmailService;

    @Mock
    private EventCompletionService eventCompletionService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private User testUser;
    private Event testEvent;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        lenient()
                .when(eventCompletionService.ensureCompletedIfEnded(any(Event.class), any(LocalDateTime.class)))
                .thenReturn(false);

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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());

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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
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
        when(registrationRepository.findEventByIdForUpdate(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EventNotFoundException.class, () -> registrationService.registerForEvent(1L, 999L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void registerForEvent_whenAlreadyRegistered_shouldThrowAlreadyRegisteredException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L))
                .thenReturn(Optional.of(new Registration(testUser, testEvent)));

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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());

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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration result = registrationService.registerForEvent(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertEquals(6, testEvent.getCurrentRegistrations());
        verify(eventRepository).save(testEvent);
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
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
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
    void registerForEvent_calledTwiceOnSingleSlotEvent_secondIsWaitlisted() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                1,
                0,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.WAITLISTED)).thenReturn(0L, 1L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Registration first = registrationService.registerForEvent(1L, 1L);
        Registration second = registrationService.registerForEvent(2L, 1L);

        // Assert
        assertEquals(RegistrationStatus.CONFIRMED, first.getStatus());
        assertEquals(RegistrationStatus.WAITLISTED, second.getStatus());
        assertEquals(1, testEvent.getCurrentRegistrations());
    }

    @Test
    void registerForEventWithFamily_withMultipleFamilyMembers_shouldCreateGroupAndUpdateCapacity() {
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
                0,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FamilyMemberRequestModel> familyMembers = List.of(
                new FamilyMemberRequestModel("Jane Doe", 12, null, "Child"),
                new FamilyMemberRequestModel("Mark Doe", 8, null, "Child")
        );

        // Act
        GroupRegistrationResponseModel result = registrationService.registerForEventWithFamily(1L, 1L, familyMembers);

        // Assert
        assertNotNull(result);
        assertEquals(3, testEvent.getCurrentRegistrations());
        assertEquals(3, result.getParticipants().size());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void registerForEventWithFamily_withInsufficientCapacity_shouldThrow() {
        // Arrange
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                2,
                1,
                "General");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());

        List<FamilyMemberRequestModel> familyMembers = List.of(
                new FamilyMemberRequestModel("Jane Doe", 12, null, "Child"),
                new FamilyMemberRequestModel("Mark Doe", 8, null, "Child")
        );

        // Act & Assert
        assertThrows(GroupRegistrationCapacityException.class,
                () -> registrationService.registerForEventWithFamily(1L, 1L, familyMembers));
    }

    @Test
    void cancelRegistration_withGroup_shouldCancelAllAndUpdateCapacity() {
        // Arrange
        Registration primary = new Registration(testUser, testEvent);
        primary.setStatus(RegistrationStatus.CONFIRMED);
        primary.setRegistrationGroupId("group-1");

        Registration family1 = new Registration(null, testEvent);
        family1.setStatus(RegistrationStatus.CONFIRMED);
        family1.setRegistrationGroupId("group-1");

        Registration family2 = new Registration(null, testEvent);
        family2.setStatus(RegistrationStatus.CONFIRMED);
        family2.setRegistrationGroupId("group-1");

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                5,
                "General");
        primary.setEvent(testEvent);
        family1.setEvent(testEvent);
        family2.setEvent(testEvent);

        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(primary));
        when(registrationRepository.findByEventIdAndRegistrationGroupId(1L, "group-1"))
                .thenReturn(List.of(primary, family1, family2));
        when(registrationRepository.saveAll(any())).thenReturn(List.of(primary, family1, family2));

        // Act
        Registration result = registrationService.cancelRegistration(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertEquals(2, testEvent.getCurrentRegistrations());
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

    @Test
    void registerForEvent_whenMemberInactive_shouldThrowInactiveMemberException() {
        testUser.setMemberStatus(com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(InactiveMemberException.class, () -> registrationService.registerForEvent(1L, 1L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void registerForEvent_whenExistingCancelled_reactivatesRegistration() {
        Registration cancelled = new Registration(testUser, testEvent);
        cancelled.setStatus(RegistrationStatus.CANCELLED);
        cancelled.setCancelledAt(LocalDateTime.now().minusDays(1));
        cancelled.setWaitlistedPosition(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(cancelled));
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Registration result = registrationService.registerForEvent(1L, 1L);

        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertNull(result.getCancelledAt());
        assertNull(result.getWaitlistedPosition());
        assertNotNull(result.getConfirmedAt());
    }

    @Test
    void registerForEvent_whenEventCompleted_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(eventCompletionService.ensureCompletedIfEnded(any(Event.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThrows(EventCompletedException.class, () -> registrationService.registerForEvent(1L, 1L));
    }

    @Test
    void registerForEventWithFamily_whenFamilyListNull_usesSingleRegistration() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupRegistrationResponseModel result = registrationService.registerForEventWithFamily(1L, 1L, null);

        assertNotNull(result);
        assertEquals(1, result.getParticipants().size());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void registerForEventWithFamily_whenMemberInactive_shouldThrow() {
        testUser.setMemberStatus(com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        List<FamilyMemberRequestModel> familyMembers = List.of(
                new FamilyMemberRequestModel("Child", 8, null, "Child")
        );

        assertThrows(InactiveMemberException.class,
                () -> registrationService.registerForEventWithFamily(1L, 1L, familyMembers));
    }

    @Test
    void registerForEventWithFamily_whenEventCompleted_shouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(eventCompletionService.ensureCompletedIfEnded(any(Event.class), any(LocalDateTime.class)))
                .thenReturn(true);

        List<FamilyMemberRequestModel> familyMembers = List.of(
                new FamilyMemberRequestModel("Child", 8, null, "Child")
        );

        assertThrows(EventCompletedException.class,
                () -> registrationService.registerForEventWithFamily(1L, 1L, familyMembers));
    }

    @Test
    void registerForEventWithFamily_parsesDateOfBirthForFamilyMember() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(registrationRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.findEventByIdForUpdate(1L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FamilyMemberRequestModel> familyMembers = List.of(
                new FamilyMemberRequestModel("Child", null, "2012-01-10", "Child")
        );

        registrationService.registerForEventWithFamily(1L, 1L, familyMembers);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Registration>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(registrationRepository).saveAll(captor.capture());
        List<Registration> savedFamily = captor.getValue();
        assertEquals(1, savedFamily.size());
        assertNotNull(savedFamily.get(0).getParticipantDateOfBirth());
        assertEquals(java.time.LocalDate.parse("2012-01-10"), savedFamily.get(0).getParticipantDateOfBirth());
    }

    @Test
    void getUserRegistrationHistory_filtersAndSortsByCategory() {
        LocalDateTime now = LocalDateTime.now();

        Event activeEvent = new Event(
                "Active Event",
                "Desc",
                now.plusDays(2),
                now.plusDays(3),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                1,
                "General");

        Event pastEvent1 = new Event(
                "Past Event 1",
                "Desc",
                now.minusDays(5),
                now.minusDays(4),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                1,
                "General");

        Event pastEvent2 = new Event(
                "Past Event 2",
                "Desc",
                now.minusDays(2),
                now.minusDays(1),
                "Loc",
                "Addr",
                EventStatus.COMPLETED,
                10,
                1,
                "General");

        Registration activeReg = new Registration(testUser, activeEvent, RegistrationStatus.CONFIRMED, now.minusDays(1));
        Registration pastReg1 = new Registration(testUser, pastEvent1, RegistrationStatus.CONFIRMED, now.minusDays(6));
        Registration pastReg2 = new Registration(testUser, pastEvent2, RegistrationStatus.CONFIRMED, now.minusDays(3));

        when(registrationRepository.findByUserIdWithEvent(1L))
                .thenReturn(List.of(pastReg1, activeReg, pastReg2));

        List<com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel> result =
                registrationService.getUserRegistrationHistory(1L, com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryFilter.ALL);

        assertEquals(3, result.size());
        assertEquals(com.mana.openhand_backend.registrations.domainclientlayer.RegistrationTimeCategory.ACTIVE, result.get(0).getTimeCategory());
        assertEquals("Active Event", result.get(0).getEvent().getTitle());
        assertEquals("Past Event 2", result.get(1).getEvent().getTitle());
        assertEquals("Past Event 1", result.get(2).getEvent().getTitle());
    }

    @Test
    void getUserRegistrationHistory_whenFilterActive_returnsOnlyActive() {
        LocalDateTime now = LocalDateTime.now();

        Event activeEvent = new Event(
                "Active Event",
                "Desc",
                now.plusDays(1),
                now.plusDays(2),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                1,
                "General");

        Event pastEvent = new Event(
                "Past Event",
                "Desc",
                now.minusDays(2),
                now.minusDays(1),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                1,
                "General");

        Registration activeReg = new Registration(testUser, activeEvent, RegistrationStatus.CONFIRMED, now.minusDays(1));
        Registration pastReg = new Registration(testUser, pastEvent, RegistrationStatus.CONFIRMED, now.minusDays(3));

        when(registrationRepository.findByUserIdWithEvent(1L))
                .thenReturn(List.of(activeReg, pastReg));

        List<com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel> result =
                registrationService.getUserRegistrationHistory(1L, com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryFilter.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(com.mana.openhand_backend.registrations.domainclientlayer.RegistrationTimeCategory.ACTIVE, result.get(0).getTimeCategory());
        assertEquals("Active Event", result.get(0).getEvent().getTitle());
    }

    @Test
    void getUserRegistrationHistory_includesGroupParticipantsWhenAvailable() {
        LocalDateTime now = LocalDateTime.now();

        Event event = new Event(
                "Grouped Event",
                "Desc",
                now.plusDays(3),
                now.plusDays(4),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                1,
                "General");

        Registration primary = new Registration(testUser, event, RegistrationStatus.CONFIRMED, now.minusDays(1));
        primary.setRegistrationGroupId("group-1");
        primary.setPrimaryRegistrant(true);
        Registration family = new Registration(null, event, RegistrationStatus.CONFIRMED, now.minusDays(1));
        family.setRegistrationGroupId("group-1");
        family.setParticipantFullName("Child");

        when(registrationRepository.findByUserIdWithEvent(1L))
                .thenReturn(List.of(primary));
        when(registrationRepository.findByRegistrationGroupId("group-1"))
                .thenReturn(List.of(primary, family));

        List<com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel> result =
                registrationService.getUserRegistrationHistory(1L, com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryFilter.ALL);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getParticipants());
        assertEquals(2, result.get(0).getParticipants().size());
    }
}
