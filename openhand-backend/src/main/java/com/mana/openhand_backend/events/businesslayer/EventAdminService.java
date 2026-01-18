package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;

public interface EventAdminService {
    Event createEvent(CreateEventRequest request);

    Event updateEvent(Long id, CreateEventRequest request);

    Event cancelEvent(Long id);
}
