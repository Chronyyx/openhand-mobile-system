package com.mana.openhand_backend.events.utils;

import com.mana.openhand_backend.events.domainclientlayer.EventAttendeeResponseModel;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;

public final class EventAttendeeResponseMapper {
    private EventAttendeeResponseMapper() {
    }

    public static EventAttendeeResponseModel toResponseModel(Registration registration) {
        if (registration == null || registration.getUser() == null) {
            return null;
        }

        return new EventAttendeeResponseModel(
                registration.getUser().getId(),
                registration.getUser().getName(),
                registration.getUser().getAge()
        );
    }
}
