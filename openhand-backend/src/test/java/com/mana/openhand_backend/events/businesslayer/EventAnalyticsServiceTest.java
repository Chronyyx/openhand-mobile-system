package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.domainclientlayer.EventAnalyticsResponseModel;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventAnalyticsServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventAnalyticsService analyticsService;

    private Event testEvent;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now();
        // Event starts in 5 days
        testEvent = new Event("Test", "Desc", baseTime.plusDays(5), baseTime.plusDays(5).plusHours(2), "Loc", "Addr",
                EventStatus.OPEN, 100, 0, "Workshop");
        testEvent.setId(1L);
    }

    @Test
    void getEventAnalytics_buildsNormalizedTimelineCorrectly() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // Mock 1 person who registered 2 days ago
        Registration r1 = new Registration(null, testEvent, RegistrationStatus.CONFIRMED, baseTime.minusDays(2));
        r1.setConfirmedAt(baseTime.minusDays(2)); // Confirmed immediately

        // Mock 1 person who waitlisted 1 day ago
        Registration r2 = new Registration(null, testEvent, RegistrationStatus.WAITLISTED, baseTime.minusDays(1));
        r2.setWaitlistedPosition(1);

        List<Registration> regs = Arrays.asList(r1, r2);

        when(registrationRepository.findByEventIdAndStatusIn(eq(1L), any()))
                .thenReturn(regs);

        // Mock empty for historical events so we only test Stage 1&2 here
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        EventAnalyticsResponseModel response = analyticsService.getEventAnalytics(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getEventId());

        List<EventAnalyticsResponseModel.DailyMetric> timeline = response.getEventTimeline();
        // Starts from min(earliest requested date, today) to start date
        // Earliest is -2 days from today, start date is +5 days from today
        // Dates: (-2), (-1), (0), (1), (2), (3), (4), (5)
        // Days before start date: 7, 6, 5, 4, 3, 2, 1, 0

        assertFalse(timeline.isEmpty());

        // At T-Minus 7 days (2 days ago), r1 confirmed
        EventAnalyticsResponseModel.DailyMetric tMinus7 = timeline.stream()
                .filter(m -> m.getDaysBeforeEvent() == 7).findFirst().orElseThrow();
        assertEquals(1, tMinus7.getConfirmed());
        assertEquals(0, tMinus7.getWaitlisted());

        // At T-Minus 6 days (1 day ago), r2 waitlisted
        EventAnalyticsResponseModel.DailyMetric tMinus6 = timeline.stream()
                .filter(m -> m.getDaysBeforeEvent() == 6).findFirst().orElseThrow();
        assertEquals(1, tMinus6.getConfirmed());
        assertEquals(1, tMinus6.getWaitlisted());

        // Check predictions (Stage 5)
        assertNotNull(response.getCurrentVelocity());
        assertNotNull(response.getPredictedFinalAttendance());
    }

}
