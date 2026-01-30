package com.mana.openhand_backend.registrations.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryEventResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationTimeCategory;
import com.mana.openhand_backend.registrations.domainclientlayer.ParticipantResponseModel;

import java.util.List;

public class RegistrationHistoryResponseMapper {

    public static RegistrationHistoryResponseModel toResponseModel(
            Registration registration,
            RegistrationTimeCategory timeCategory,
            List<ParticipantResponseModel> participants) {
        if (registration == null) {
            return null;
        }

        String createdAt = registration.getRequestedAt() != null
                ? registration.getRequestedAt().toString()
                : null;

        String status = registration.getStatus() != null
                ? registration.getStatus().name()
                : null;

        Event event = registration.getEvent();

        RegistrationHistoryEventResponseModel eventResponse = null;
        if (event != null) {
            String startDateTime = event.getStartDateTime() != null
                    ? event.getStartDateTime().toString()
                    : null;
            String endDateTime = event.getEndDateTime() != null
                    ? event.getEndDateTime().toString()
                    : null;
            eventResponse = new RegistrationHistoryEventResponseModel(
                    event.getId(),
                    event.getTitle(),
                    startDateTime,
                    endDateTime,
                    event.getLocationName()
            );
        }

        return new RegistrationHistoryResponseModel(
                registration.getId(),
                status,
                createdAt,
                timeCategory,
                eventResponse,
                participants
        );
    }
}
