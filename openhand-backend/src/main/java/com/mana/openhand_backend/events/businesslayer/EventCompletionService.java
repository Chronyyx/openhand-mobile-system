package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventCompletionService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventCompletionService(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    private Event finishEvent(Event event) {
        if (event.getStatus() == EventStatus.COMPLETED) {
            return event;
        }
        event.setStatus(EventStatus.COMPLETED);
        event.setCompletedAt(LocalDateTime.now());

        long waitlistCount = registrationRepository.countByEventIdAndStatus(event.getId(),
                RegistrationStatus.WAITLISTED);
        event.setFinalWaitlistCount((int) waitlistCount);

        return event;
    }

    @Transactional
    public void refreshCompletedEvents(LocalDateTime now) {
        List<Event> toComplete = eventRepository
                .findByEndDateTimeNotNullAndEndDateTimeLessThanEqualAndStatusNot(now, EventStatus.COMPLETED);

        if (toComplete.isEmpty()) {
            return;
        }

        toComplete.forEach(this::finishEvent);
        eventRepository.saveAll(toComplete);
    }

    @Transactional
    public boolean ensureCompletedIfEnded(Event event, LocalDateTime now) {
        if (event.getStatus() == EventStatus.COMPLETED) {
            return true;
        }

        if (event.getEndDateTime() != null && !event.getEndDateTime().isAfter(now)) {
            finishEvent(event);
            eventRepository.save(event);
            return true;
        }

        return false;
    }

    @Transactional
    public Event markCompleted(Event event) {
        if (event.getStatus() == EventStatus.COMPLETED) {
            return event;
        }

        finishEvent(event);
        return eventRepository.save(event);
    }
}
