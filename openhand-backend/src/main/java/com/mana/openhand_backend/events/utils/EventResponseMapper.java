package com.mana.openhand_backend.events.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;

public class EventResponseMapper {

    public static EventResponseModel toResponseModel(Event event) {
        if (event == null) {
            return null;
        }

        String start = event.getStartDateTime() != null
                ? event.getStartDateTime().toString()
                : null;

        String end = event.getEndDateTime() != null
                ? event.getEndDateTime().toString()
                : null;

        String status = event.getStatus() != null
                ? event.getStatus().name()
                : null;

        return new EventResponseModel(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                start,
                end,
                event.getLocationName(),
                event.getAddress(),
                status,
                event.getMaxCapacity(),
                event.getCurrentRegistrations()
        );
    }
}
