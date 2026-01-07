package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        return eventRepository.findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(startOfToday);
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

        return new RegistrationSummaryResponseModel(
                eventId,
                (int) confirmedCount,
                (int) waitlistedCount,
                maxCapacity,
                remainingSpots,
                percentageFull
        );
    }
}
