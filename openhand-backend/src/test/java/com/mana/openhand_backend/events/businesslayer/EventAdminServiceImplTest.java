package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private SendGridEmailService sendGridEmailService;

    @Mock
    private EventCompletionService eventCompletionService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EventAdminServiceImpl eventAdminService;

    private CreateEventRequest buildRequest(String title, String endDateTime, Integer maxCapacity, String category) {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle(title);
        request.setDescription("Desc");
        request.setStartDateTime("2026-01-01T18:00");
        request.setEndDateTime(endDateTime);
        request.setLocationName("Loc");
        request.setAddress("Addr");
        request.setMaxCapacity(maxCapacity);
        request.setCategory(category);
        return request;
    }

    @Test
    void createEvent_withValidInput_trimsValuesAndPersists() {
        CreateEventRequest request = buildRequest("  Title  ", "2026-01-01T20:00", 25, "  GENERAL ");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventAdminService.createEvent(request);

        assertEquals("Title", result.getTitle());
        assertEquals("Desc", result.getDescription());
        assertEquals(LocalDateTime.parse("2026-01-01T18:00"), result.getStartDateTime());
        assertEquals(LocalDateTime.parse("2026-01-01T20:00"), result.getEndDateTime());
        assertEquals("Loc", result.getLocationName());
        assertEquals("Addr", result.getAddress());
        assertEquals(EventStatus.OPEN, result.getStatus());
        assertEquals(Integer.valueOf(25), result.getMaxCapacity());
        assertEquals(Integer.valueOf(0), result.getCurrentRegistrations());
        assertEquals("GENERAL", result.getCategory());

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertEquals("Title", saved.getValue().getTitle());
    }

    @Test
    void createEvent_withBlankCategory_setsCategoryToNull() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 10, "   ");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventAdminService.createEvent(request);

        assertNull(result.getCategory());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void createEvent_withNoCapacity_allowsNullCapacity() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", null, "GENERAL");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventAdminService.createEvent(request);

        assertNull(result.getMaxCapacity());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_withEndBeforeStart_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T17:00", 10, "GENERAL");

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEvent_withNonPositiveCapacity_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 0, "GENERAL");

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    private Event existingEvent(int currentRegistrations) {
        Event event = new Event(
                "Existing",
                "Existing desc",
                LocalDateTime.parse("2026-01-01T18:00"),
                LocalDateTime.parse("2026-01-01T20:00"),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                50,
                currentRegistrations,
                "GENERAL");
        return event;
    }

    @Test
    void updateEvent_whenNotFound_throwsNoSuchElementException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 10, "GENERAL");
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> eventAdminService.updateEvent(99L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_withEndBeforeStart_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T17:00", 10, "GENERAL");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent(3)));

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.updateEvent(1L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_whenCompleted_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 10, "GENERAL");
        Event existing = existingEvent(3);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventCompletionService.ensureCompletedIfEnded(eq(existing), any(LocalDateTime.class))).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.updateEvent(1L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_withMaxCapacityLessThanCurrent_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 2, "GENERAL");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent(3)));

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.updateEvent(1L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_withNonPositiveCapacity_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 0, "GENERAL");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent(0)));

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.updateEvent(1L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_withNullCategory_setsNull() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 10, null);
        Event existing = existingEvent(1);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertNull(updated.getCategory());
    }

    @Test
    void updateEvent_withNullCurrentRegistrations_defaultsToZero() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 10, "GENERAL");
        Event existing = existingEvent(0);
        existing.setCurrentRegistrations(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertEquals(EventStatus.OPEN, updated.getStatus());
    }

    @Test
    void updateEvent_setsStatusToFullWhenAtCapacity() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 5, "GENERAL");
        Event existing = existingEvent(5);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertEquals(EventStatus.FULL, updated.getStatus());
        assertEquals(Integer.valueOf(5), updated.getMaxCapacity());
        assertEquals("Updated", updated.getTitle());
        verify(eventRepository, times(1)).save(existing);
    }

    @Test
    void updateEvent_setsStatusToNearlyFullAtEightyPercent() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 5, "GENERAL");
        Event existing = existingEvent(4);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertEquals(EventStatus.NEARLY_FULL, updated.getStatus());
        assertEquals(Integer.valueOf(5), updated.getMaxCapacity());
        verify(eventRepository, times(1)).save(existing);
    }

    @Test
    void updateEvent_setsStatusToOpenWhenNoCapacityLimit() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", null, "GENERAL");
        Event existing = existingEvent(1);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertEquals(EventStatus.OPEN, updated.getStatus());
        assertNull(updated.getMaxCapacity());
    }

    @Test
    void updateEvent_notifiesWhenScheduleChanges() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 10, "GENERAL");
        Event existing = existingEvent(1);
        ReflectionTestUtils.setField(existing, "id", 1L);
        existing.setStartDateTime(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = new User();
        user.setEmail("member@example.com");
        user.setName("Member");

        Registration active = new Registration(user, existing);
        active.setStatus(RegistrationStatus.CONFIRMED);

        Registration cancelled = new Registration(user, existing);
        cancelled.setStatus(RegistrationStatus.CANCELLED);

        when(registrationRepository.findByEventId(1L)).thenReturn(List.of(active, cancelled));

        eventAdminService.updateEvent(1L, request);

        verify(sendGridEmailService).sendCancellationOrUpdate(
                eq("member@example.com"),
                eq("Member"),
                eq("Updated"),
                contains("Event schedule updated"),
                eq("en"));
    }

    @Test
    void notifyScheduleChange_handlesSendFailures() {
        Event event = existingEvent(1);
        ReflectionTestUtils.setField(event, "id", 5L);

        User firstUser = new User();
        firstUser.setEmail("first@example.com");
        firstUser.setName("First");
        User secondUser = new User();
        secondUser.setEmail("second@example.com");
        secondUser.setName("Second");

        Registration first = new Registration(firstUser, event);
        first.setStatus(RegistrationStatus.CONFIRMED);
        Registration second = new Registration(secondUser, event);
        second.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findByEventId(5L)).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("send failed"))
                .when(sendGridEmailService)
                .sendCancellationOrUpdate(eq("first@example.com"), any(), any(), any(), any());

        ReflectionTestUtils.invokeMethod(eventAdminService, "notifyScheduleChange", event);

        verify(sendGridEmailService).sendCancellationOrUpdate(
                eq("first@example.com"),
                eq("First"),
                eq(event.getTitle()),
                contains("Event schedule updated"),
                eq("en"));
        verify(sendGridEmailService).sendCancellationOrUpdate(
                eq("second@example.com"),
                eq("Second"),
                eq(event.getTitle()),
                contains("Event schedule updated"),
                eq("en"));
    }

    @Test
    void notifyScheduleChange_handlesRepositoryFailures() {
        Event event = existingEvent(1);
        ReflectionTestUtils.setField(event, "id", 9L);
        when(registrationRepository.findByEventId(9L))
                .thenThrow(new RuntimeException("db down"));

        ReflectionTestUtils.invokeMethod(eventAdminService, "notifyScheduleChange", event);

        verifyNoInteractions(sendGridEmailService);
    }

    @Test
    void scheduleChanged_handlesNulls() {
        LocalDateTime now = LocalDateTime.now();

        boolean bothNull = ReflectionTestUtils.invokeMethod(eventAdminService, "scheduleChanged", null, null);
        boolean originalNull = ReflectionTestUtils.invokeMethod(eventAdminService, "scheduleChanged", null, now);
        boolean same = ReflectionTestUtils.invokeMethod(eventAdminService, "scheduleChanged", now, now);

        assertFalse(bothNull);
        assertTrue(originalNull);
        assertFalse(same);
    }

    @Test
    void determineStatus_coversAllBranches() {
        EventStatus openUnlimited = ReflectionTestUtils.invokeMethod(eventAdminService, "determineStatus", null, 1);
        EventStatus full = ReflectionTestUtils.invokeMethod(eventAdminService, "determineStatus", 5, 5);
        EventStatus nearlyFull = ReflectionTestUtils.invokeMethod(eventAdminService, "determineStatus", 10, 8);
        EventStatus open = ReflectionTestUtils.invokeMethod(eventAdminService, "determineStatus", 10, 2);

        assertEquals(EventStatus.OPEN, openUnlimited);
        assertEquals(EventStatus.FULL, full);
        assertEquals(EventStatus.NEARLY_FULL, nearlyFull);
        assertEquals(EventStatus.OPEN, open);
    }

    @Test
    void cancelEvent_cancelsEventAndRegistrationsAndNotifies() {
        Event event = existingEvent(2);
        ReflectionTestUtils.setField(event, "id", 1L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setName("User");

        Registration active = new Registration(user, event);
        active.setStatus(RegistrationStatus.CONFIRMED);

        // Mock registrations
        when(registrationRepository.findByEventId(1L)).thenReturn(List.of(active));
        when(registrationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Event cancelledEvents = eventAdminService.cancelEvent(1L);

        assertEquals(EventStatus.CANCELLED, cancelledEvents.getStatus());
        assertEquals(RegistrationStatus.CANCELLED, active.getStatus());

        verify(eventRepository).save(event);
        verify(registrationRepository).saveAll(any());

        // Verify notifications
        verify(sendGridEmailService).sendCancellationOrUpdate(
                eq("user@example.com"),
                eq("User"),
                eq("Existing"),
                eq("Event Cancelled"),
                eq("en"));
        verify(notificationService).createNotification(
                eq(10L),
                eq(1L),
                eq("CANCELLATION"),
                eq("en"));
    }

    @Test
    void cancelEvent_whenAlreadyCancelled_returnsWithoutUpdates() {
        Event event = existingEvent(1);
        event.setStatus(EventStatus.CANCELLED);
        ReflectionTestUtils.setField(event, "id", 2L);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));

        Event result = eventAdminService.cancelEvent(2L);

        assertEquals(EventStatus.CANCELLED, result.getStatus());
        verifyNoInteractions(registrationRepository);
    }

    @Test
    void notifyScheduleChange_handlesNotificationFailures() {
        Event event = existingEvent(1);
        ReflectionTestUtils.setField(event, "id", 7L);

        User user = new User();
        user.setEmail("member@example.com");
        user.setName("Member");
        user.setId(11L);
        Registration registration = new Registration(user, event);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findByEventId(7L)).thenReturn(List.of(registration));
        doThrow(new RuntimeException("notify failed"))
                .when(notificationService)
                .createNotification(anyLong(), anyLong(), any(), any());

        ReflectionTestUtils.invokeMethod(eventAdminService, "notifyScheduleChange", event);

        verify(notificationService).createNotification(eq(11L), eq(7L), eq("EVENT_UPDATE"), eq("en"));
    }

    @Test
    void notifyCancellation_handlesEmailAndNotificationFailures() {
        Event event = existingEvent(1);
        ReflectionTestUtils.setField(event, "id", 8L);

        User user = new User();
        user.setEmail("member@example.com");
        user.setName("Member");
        user.setId(12L);
        Registration registration = new Registration(user, event);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        doThrow(new RuntimeException("email failed"))
                .when(sendGridEmailService)
                .sendCancellationOrUpdate(any(), any(), any(), any(), any());
        doThrow(new RuntimeException("notify failed"))
                .when(notificationService)
                .createNotification(anyLong(), anyLong(), any(), any());

        ReflectionTestUtils.invokeMethod(eventAdminService, "notifyCancellation", event, List.of(registration));

        verify(sendGridEmailService).sendCancellationOrUpdate(
                eq("member@example.com"),
                eq("Member"),
                eq("Existing"),
                eq("Event Cancelled"),
                eq("en"));
        verify(notificationService).createNotification(eq(12L), eq(8L), eq("CANCELLATION"), eq("en"));
    }
}
