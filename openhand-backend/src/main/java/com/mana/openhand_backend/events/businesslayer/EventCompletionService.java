package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventCompletionService {

    private final EventRepository eventRepository;

    public EventCompletionService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void refreshCompletedEvents(LocalDateTime now) {
        List<Event> toComplete = eventRepository
                .findByEndDateTimeNotNullAndEndDateTimeLessThanEqualAndStatusNot(now, EventStatus.COMPLETED);

        if (toComplete.isEmpty()) {
            return;
        }

        toComplete.forEach(event -> event.setStatus(EventStatus.COMPLETED));
        eventRepository.saveAll(toComplete);
    }

    @Transactional
    public boolean ensureCompletedIfEnded(Event event, LocalDateTime now) {
        if (event.getStatus() == EventStatus.COMPLETED) {
            return true;
        }

        if (event.getEndDateTime() != null && !event.getEndDateTime().isAfter(now)) {
            event.setStatus(EventStatus.COMPLETED);
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

        event.setStatus(EventStatus.COMPLETED);
        return eventRepository.save(event);
    }
}
