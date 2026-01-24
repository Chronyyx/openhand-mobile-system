package com.mana.openhand_backend.attendance.businesslayer;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventAttendeesResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventSummaryResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceUpdateResponseModel;
import com.mana.openhand_backend.attendance.utils.AttendanceCheckInNotAllowedException;
import com.mana.openhand_backend.attendance.utils.AttendanceRegistrationNotFoundException;
import com.mana.openhand_backend.events.businesslayer.EventCompletionService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCompletionService eventCompletionService;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @Test
    void getAttendanceEvents_mapsOccupancyAndDates() {
        Event event = buildEvent(1L, 10);
        event.setTitle("Event A");
        event.setStartDateTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        event.setEndDateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        event.setLocationName("Hall");
        event.setAddress("123 Street");
        event.setStatus(EventStatus.OPEN);

        when(eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDateTime"))).thenReturn(List.of(event));
        when(registrationRepository.countByEventIdAndStatusNot(1L, RegistrationStatus.CANCELLED)).thenReturn(5L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(1L)).thenReturn(2L);

        List<AttendanceEventSummaryResponseModel> result = attendanceService.getAttendanceEvents();

        assertEquals(1, result.size());
        AttendanceEventSummaryResponseModel summary = result.get(0);
        assertEquals(1L, summary.getEventId());
        assertEquals("Event A", summary.getTitle());
        assertEquals("Hall", summary.getLocationName());
        assertEquals(5, summary.getRegisteredCount());
        assertEquals(2, summary.getCheckedInCount());
        assertEquals(20.0, summary.getOccupancyPercent());

        verify(eventCompletionService).refreshCompletedEvents(any(LocalDateTime.class));
    }

    @Test
    void getAttendanceEvents_handlesNullCapacity() {
        Event event = buildEvent(2L, null);
        event.setTitle("No Cap");
        event.setStartDateTime(LocalDateTime.of(2025, 1, 2, 9, 0));

        when(eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDateTime"))).thenReturn(List.of(event));
        when(registrationRepository.countByEventIdAndStatusNot(2L, RegistrationStatus.CANCELLED)).thenReturn(0L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(2L)).thenReturn(0L);

        AttendanceEventSummaryResponseModel summary = attendanceService.getAttendanceEvents().get(0);

        assertNull(summary.getOccupancyPercent());
    }

    @Test
    void getEventAttendees_whenEventMissing_throwsNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> attendanceService.getEventAttendees(99L));
    }

    @Test
    void getEventAttendees_countsCheckedIn() {
        Event event = buildEvent(3L, 50);
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        Registration reg1 = buildRegistration(11L, event, RegistrationStatus.CONFIRMED, true);
        Registration reg2 = buildRegistration(12L, event, RegistrationStatus.CONFIRMED, false);

        when(registrationRepository.findByEventIdAndStatusNot(3L, RegistrationStatus.CANCELLED))
                .thenReturn(List.of(reg1, reg2));

        AttendanceEventAttendeesResponseModel response = attendanceService.getEventAttendees(3L);

        assertEquals(2, response.getRegisteredCount());
        assertEquals(1, response.getCheckedInCount());
        assertEquals(2, response.getAttendees().size());
    }

    @Test
    void checkInAttendee_whenRegistrationMissing_throwsNotFound() {
        when(registrationRepository.findByUserIdAndEventId(5L, 6L)).thenReturn(Optional.empty());

        assertThrows(AttendanceRegistrationNotFoundException.class,
                () -> attendanceService.checkInAttendee(6L, 5L));
    }

    @Test
    void checkInAttendee_whenCancelled_throwsNotAllowed() {
        Event event = buildEvent(7L, 10);
        Registration registration = buildRegistration(21L, event, RegistrationStatus.CANCELLED, false);
        when(registrationRepository.findByUserIdAndEventId(21L, 7L)).thenReturn(Optional.of(registration));

        assertThrows(AttendanceCheckInNotAllowedException.class,
                () -> attendanceService.checkInAttendee(7L, 21L));
    }

    @Test
    void checkInAttendee_setsCheckedInAndPublishes() {
        Event event = buildEvent(8L, 10);
        Registration registration = buildRegistration(31L, event, RegistrationStatus.CONFIRMED, false);
        when(registrationRepository.findByUserIdAndEventId(31L, 8L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(registration)).thenReturn(registration);
        when(registrationRepository.countByEventIdAndStatusNot(8L, RegistrationStatus.CANCELLED)).thenReturn(3L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(8L)).thenReturn(1L);

        AttendanceUpdateResponseModel update = attendanceService.checkInAttendee(8L, 31L);

        assertTrue(update.isCheckedIn());
        assertNotNull(update.getCheckedInAt());
        assertEquals(10.0, update.getOccupancyPercent());

        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(AttendanceUpdateResponseModel.class));
    }

    @Test
    void undoCheckInAttendee_clearsCheckedInAndPublishes() {
        Event event = buildEvent(9L, 10);
        Registration registration = buildRegistration(41L, event, RegistrationStatus.CONFIRMED, true);
        when(registrationRepository.findByUserIdAndEventId(41L, 9L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(registration)).thenReturn(registration);
        when(registrationRepository.countByEventIdAndStatusNot(9L, RegistrationStatus.CANCELLED)).thenReturn(2L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(9L)).thenReturn(0L);

        AttendanceUpdateResponseModel update = attendanceService.undoCheckInAttendee(9L, 41L);

        assertFalse(update.isCheckedIn());
        assertNull(update.getCheckedInAt());
        assertEquals(0.0, update.getOccupancyPercent());

        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(AttendanceUpdateResponseModel.class));
    }

    @Test
    void checkInAttendee_doesNotOverwriteExistingTimestamp() {
        Event event = buildEvent(10L, 10);
        Registration registration = buildRegistration(51L, event, RegistrationStatus.CONFIRMED, true);
        LocalDateTime existing = registration.getCheckedInAt();
        when(registrationRepository.findByUserIdAndEventId(51L, 10L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(registration)).thenReturn(registration);
        when(registrationRepository.countByEventIdAndStatusNot(10L, RegistrationStatus.CANCELLED)).thenReturn(1L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(10L)).thenReturn(1L);

        attendanceService.checkInAttendee(10L, 51L);

        assertEquals(existing, registration.getCheckedInAt());
    }

    private Event buildEvent(Long id, Integer maxCapacity) {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now(),
                null,
                "Location",
                "Address",
                EventStatus.OPEN,
                maxCapacity,
                0,
                "CATEGORY"
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private Registration buildRegistration(Long userId, Event event, RegistrationStatus status, boolean checkedIn) {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        user.setId(userId);
        Registration registration = new Registration(user, event, status, LocalDateTime.now());
        if (checkedIn) {
            registration.setCheckedInAt(LocalDateTime.now().minusMinutes(5));
        }
        registration.setStatus(status);
        return registration;
    }
}
