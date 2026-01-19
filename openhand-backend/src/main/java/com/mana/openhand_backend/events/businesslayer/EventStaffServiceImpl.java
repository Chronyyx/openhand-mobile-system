package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventStaffServiceImpl implements EventStaffService {

    private final EventRepository eventRepository;
    private final EventCompletionService eventCompletionService;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;

    public EventStaffServiceImpl(EventRepository eventRepository,
                                 EventCompletionService eventCompletionService,
                                 RegistrationRepository registrationRepository,
                                 NotificationRepository notificationRepository) {
        this.eventRepository = eventRepository;
        this.eventCompletionService = eventCompletionService;
        this.registrationRepository = registrationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Event> getEventsForStaff() {
        eventCompletionService.refreshCompletedEvents(LocalDateTime.now());
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDateTime"));
    }

    @Override
    public Event markEventCompleted(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return eventCompletionService.markCompleted(event);
    }

    @Override
    @Transactional
    public void deleteArchivedEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed events can be deleted.");
        }

        notificationRepository.deleteByEventId(eventId);
        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

}
