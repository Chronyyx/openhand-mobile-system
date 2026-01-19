package com.mana.openhand_backend.attendance.businesslayer;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventSummaryResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceUpdateResponseModel;
import com.mana.openhand_backend.events.businesslayer.EventCompletionService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private AttendanceServiceImpl attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceServiceImpl(
                eventRepository,
                eventCompletionService,
                registrationRepository,
                messagingTemplate
        );
    }

    @Test
    void getAttendanceEvents_returnsSummariesWithCounts() {
        Event event = new Event(
                "Training",
                "Event description",
                LocalDateTime.of(2030, 6, 1, 10, 0),
                null,
                "Centre MANA",
                "123 Main Street",
                EventStatus.OPEN,
                100,
                0,
                "GALA"
        );
        ReflectionTestUtils.setField(event, "id", 11L);

        when(eventRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(event));
        when(registrationRepository.countByEventIdAndStatusNot(11L, RegistrationStatus.CANCELLED)).thenReturn(20L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(11L)).thenReturn(5L);

        List<AttendanceEventSummaryResponseModel> summaries = attendanceService.getAttendanceEvents();

        assertEquals(1, summaries.size());
        AttendanceEventSummaryResponseModel summary = summaries.get(0);
        assertEquals(11L, summary.getEventId());
        assertEquals(20, summary.getRegisteredCount());
        assertEquals(5, summary.getCheckedInCount());
        assertEquals(5.0, summary.getOccupancyPercent());
        verify(eventCompletionService).refreshCompletedEvents(any(LocalDateTime.class));
    }

    @Test
    void checkInAttendee_setsCheckedInAtAndPublishesUpdate() {
        Event event = new Event(
                "Training",
                "Event description",
                LocalDateTime.of(2030, 6, 1, 10, 0),
                null,
                "Centre MANA",
                "123 Main Street",
                EventStatus.OPEN,
                10,
                0,
                "GALA"
        );
        ReflectionTestUtils.setField(event, "id", 1L);

        User user = new User();
        user.setId(7L);
        user.setName("Ada Lovelace");
        user.setEmail("ada@mana.org");

        Registration registration = new Registration(user, event);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findByUserIdAndEventId(7L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRepository.countByEventIdAndStatusNot(1L, RegistrationStatus.CANCELLED)).thenReturn(3L);
        when(registrationRepository.countByEventIdAndCheckedInAtIsNotNull(1L)).thenReturn(2L);

        AttendanceUpdateResponseModel update = attendanceService.checkInAttendee(1L, 7L);

        assertTrue(update.isCheckedIn());
        assertEquals(3, update.getRegisteredCount());
        assertEquals(2, update.getCheckedInCount());
        assertEquals(20.0, update.getOccupancyPercent());
        assertNotNull(registration.getCheckedInAt());

        verify(messagingTemplate).convertAndSend(eq("/topic/attendance/events"), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/attendance/events/1"), any());
    }
}
