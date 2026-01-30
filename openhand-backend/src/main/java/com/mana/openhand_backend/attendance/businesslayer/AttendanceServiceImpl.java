package com.mana.openhand_backend.attendance.businesslayer;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceAttendeeResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventAttendeesResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventSummaryResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceUpdateResponseModel;
import com.mana.openhand_backend.attendance.utils.AttendanceCheckInNotAllowedException;
import com.mana.openhand_backend.attendance.utils.AttendanceRegistrationNotFoundException;
import com.mana.openhand_backend.events.businesslayer.EventCompletionService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    private final EventRepository eventRepository;
    private final EventCompletionService eventCompletionService;
    private final RegistrationRepository registrationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AttendanceServiceImpl(EventRepository eventRepository,
                                 EventCompletionService eventCompletionService,
                                 RegistrationRepository registrationRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.eventRepository = eventRepository;
        this.eventCompletionService = eventCompletionService;
        this.registrationRepository = registrationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<AttendanceEventSummaryResponseModel> getAttendanceEvents() {
        eventCompletionService.refreshCompletedEvents(LocalDateTime.now());
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDateTime"));
        return events.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceEventAttendeesResponseModel getEventAttendees(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        List<Registration> registrations = registrationRepository.findByEventIdAndStatusNot(
                eventId,
                RegistrationStatus.CANCELLED
        );

        List<AttendanceAttendeeResponseModel> attendees = registrations.stream()
                .filter(registration -> registration.getUser() != null)
                .map(this::toAttendeeResponse)
                .collect(Collectors.toList());

        int checkedInCount = (int) registrations.stream()
                .filter(registration -> registration.getUser() != null)
                .filter(registration -> registration.getCheckedInAt() != null)
                .count();

        return new AttendanceEventAttendeesResponseModel(
                event.getId(),
                attendees.size(),
                checkedInCount,
                attendees
        );
    }

    @Override
    @Transactional
    public AttendanceUpdateResponseModel checkInAttendee(Long eventId, Long userId) {
        Registration registration = registrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new AttendanceRegistrationNotFoundException(eventId, userId));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new AttendanceCheckInNotAllowedException(eventId, userId);
        }

        if (registration.getCheckedInAt() == null) {
            registration.setCheckedInAt(LocalDateTime.now());
        }

        Registration saved = registrationRepository.save(registration);
        AttendanceUpdateResponseModel update = buildUpdateResponse(saved, eventId, userId);
        publishUpdate(update);
        return update;
    }

    @Override
    @Transactional
    public AttendanceUpdateResponseModel undoCheckInAttendee(Long eventId, Long userId) {
        Registration registration = registrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new AttendanceRegistrationNotFoundException(eventId, userId));

        if (registration.getCheckedInAt() != null) {
            registration.setCheckedInAt(null);
        }

        Registration saved = registrationRepository.save(registration);
        AttendanceUpdateResponseModel update = buildUpdateResponse(saved, eventId, userId);
        publishUpdate(update);
        return update;
    }

    private AttendanceEventSummaryResponseModel toSummaryResponse(Event event) {
        long registeredCount = registrationRepository.countByEventIdAndStatusNot(
                event.getId(),
                RegistrationStatus.CANCELLED
        );
        long checkedInCount = registrationRepository.countByEventIdAndCheckedInAtIsNotNull(event.getId());
        Double occupancyPercent = calculateOccupancy(event.getMaxCapacity(), checkedInCount);

        String start = event.getStartDateTime() != null
                ? event.getStartDateTime().toString()
                : null;
        String end = event.getEndDateTime() != null
                ? event.getEndDateTime().toString()
                : null;
        String status = event.getStatus() != null
                ? event.getStatus().name()
                : null;

        return new AttendanceEventSummaryResponseModel(
                event.getId(),
                event.getTitle(),
                start,
                end,
                event.getLocationName(),
                event.getAddress(),
                status,
                event.getMaxCapacity(),
                (int) registeredCount,
                (int) checkedInCount,
                occupancyPercent
        );
    }

    private AttendanceAttendeeResponseModel toAttendeeResponse(Registration registration) {
        String checkedInAt = registration.getCheckedInAt() != null
                ? registration.getCheckedInAt().toString()
                : null;

        return new AttendanceAttendeeResponseModel(
                registration.getUser().getId(),
                registration.getUser().getName(),
                registration.getUser().getEmail(),
                registration.getStatus().name(),
                registration.getCheckedInAt() != null,
                checkedInAt
        );
    }

    private AttendanceUpdateResponseModel buildUpdateResponse(Registration registration, Long eventId, Long userId) {
        Event event = registration.getEvent();
        long registeredCount = registrationRepository.countByEventIdAndStatusNot(eventId, RegistrationStatus.CANCELLED);
        long checkedInCount = registrationRepository.countByEventIdAndCheckedInAtIsNotNull(eventId);
        Double occupancyPercent = calculateOccupancy(event.getMaxCapacity(), checkedInCount);

        String checkedInAt = registration.getCheckedInAt() != null
                ? registration.getCheckedInAt().toString()
                : null;

        return new AttendanceUpdateResponseModel(
                eventId,
                userId,
                registration.getCheckedInAt() != null,
                checkedInAt,
                (int) registeredCount,
                (int) checkedInCount,
                occupancyPercent
        );
    }

    private Double calculateOccupancy(Integer maxCapacity, long checkedInCount) {
        if (maxCapacity == null) {
            return null;
        }
        if (maxCapacity <= 0) {
            return 0.0;
        }
        return (checkedInCount * 100.0) / maxCapacity;
    }

    private void publishUpdate(AttendanceUpdateResponseModel update) {
        try {
            messagingTemplate.convertAndSend("/topic/attendance/events", update);
            messagingTemplate.convertAndSend("/topic/attendance/events/" + update.getEventId(), update);
        } catch (Exception ex) {
            logger.error("Failed to publish attendance update: {}", ex.getMessage());
        }
    }
}
