package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.AttendeeResponseModel;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventServiceImpl(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public List<Event> getUpcomingEvents() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<Event> upcoming = eventRepository.findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(startOfToday);

        // Fallback: if no future events, surface past events (e.g., seed data) so the list is never empty
        if (upcoming.isEmpty()) {
            return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDateTime"));
        }

        return upcoming;
    }

    @Override
    @SuppressWarnings("null")
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Override
    public RegistrationSummaryResponseModel getRegistrationSummary(Long eventId) {
        Event event = getEventById(eventId);

        // Prefer the denormalized counter stored on the event for parity with the events list.
        // If it's null (legacy data), fall back to live DB counting.
        long confirmedCount = event.getCurrentRegistrations() != null
                ? event.getCurrentRegistrations()
                : registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);

        // Count waitlisted registrations from DB (not stored on event)
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);

        // Calculate remaining spots and percentage
        Integer maxCapacity = event.getMaxCapacity();
        Integer remainingSpots = null;
        Double percentageFull = null;

        if (maxCapacity != null) {
            remainingSpots = Math.max(0, maxCapacity - (int) confirmedCount);
            if (maxCapacity > 0) {
                percentageFull = (confirmedCount * 100.0) / maxCapacity;
            } else {
                percentageFull = 0.0;
            }
        }

        // Fetch all registrations for this event to populate attendees list
        List<Registration> registrations = registrationRepository.findByEventIdAndStatusIn(eventId, 
                List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED));
        
        // Convert to attendee response models with member status
        List<AttendeeResponseModel> attendees = registrations.stream()
                .map(registration -> new AttendeeResponseModel(
                        registration.getUser().getId(),
                        registration.getUser().getName(),
                        registration.getUser().getEmail(),
                        registration.getStatus().toString(),
                        registration.getUser().getMemberStatus() != null ? 
                                registration.getUser().getMemberStatus().toString() : "ACTIVE",
                        registration.getWaitlistedPosition(),
                        registration.getRequestedAt() != null ? registration.getRequestedAt().toString() : null,
                        registration.getConfirmedAt() != null ? registration.getConfirmedAt().toString() : null
                ))
                .collect(Collectors.toList());

        return new RegistrationSummaryResponseModel(
                eventId,
                (int) confirmedCount,
                (int) waitlistedCount,
                maxCapacity,
                remainingSpots,
                percentageFull,
                attendees
        );
    }
}
