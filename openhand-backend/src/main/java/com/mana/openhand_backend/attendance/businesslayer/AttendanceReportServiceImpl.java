package com.mana.openhand_backend.attendance.businesslayer;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceReportResponseModel;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceReportServiceImpl implements AttendanceReportService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public AttendanceReportServiceImpl(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public List<AttendanceReportResponseModel> getAttendanceReports(LocalDate startDate, LocalDate endDate, Long eventId) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Event> events = resolveEvents(startDateTime, endDateTime, eventId);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Integer> attendedCountByEvent = registrationRepository
                .countAttendanceGroupedByEventIds(eventIds, RegistrationStatus.CANCELLED)
                .stream()
                .collect(Collectors.toMap(
                        RegistrationRepository.EventCountProjection::getEventId,
                        value -> Math.toIntExact(value.getTotal())
                ));

        Map<Long, Integer> registeredCountByEvent = registrationRepository
                .countRegistrationsGroupedByEventIds(eventIds, RegistrationStatus.CANCELLED)
                .stream()
                .collect(Collectors.toMap(
                        RegistrationRepository.EventCountProjection::getEventId,
                        value -> Math.toIntExact(value.getTotal())
                ));

        return events.stream()
                .map(event -> buildResponse(event, attendedCountByEvent, registeredCountByEvent))
                .toList();
    }

    private List<Event> resolveEvents(LocalDateTime startDateTime, LocalDateTime endDateTime, Long eventId) {
        if (eventId == null) {
            return eventRepository.findByStartDateTimeBetweenOrderByStartDateTimeAsc(startDateTime, endDateTime);
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getStartDateTime() == null
                || event.getStartDateTime().isBefore(startDateTime)
                || event.getStartDateTime().isAfter(endDateTime)) {
            return Collections.emptyList();
        }

        return List.of(event);
    }

    private AttendanceReportResponseModel buildResponse(
            Event event,
            Map<Long, Integer> attendedCountByEvent,
            Map<Long, Integer> registeredCountByEvent) {
        int totalAttended = attendedCountByEvent.getOrDefault(event.getId(), 0);
        int totalRegistered = registeredCountByEvent.getOrDefault(event.getId(), 0);
        double attendanceRate = totalRegistered == 0 ? 0.0 : (double) totalAttended / totalRegistered;

        return new AttendanceReportResponseModel(
                event.getId(),
                event.getTitle(),
                event.getStartDateTime(),
                totalAttended,
                totalRegistered,
                attendanceRate
        );
    }
}
