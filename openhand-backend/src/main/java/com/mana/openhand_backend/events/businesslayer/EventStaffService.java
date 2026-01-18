package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;

import java.util.List;

public interface EventStaffService {

    List<Event> getEventsForStaff();

    Event markEventCompleted(Long eventId);

    void deleteArchivedEvent(Long eventId);
}
