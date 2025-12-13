package com.mana.openhand_backend.registrations.utils;

import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationResponseModel;

public class RegistrationResponseMapper {

    public static RegistrationResponseModel toResponseModel(Registration registration) {
        if (registration == null) {
            return null;
        }

        String requestedAt = registration.getRequestedAt() != null
                ? registration.getRequestedAt().toString()
                : null;

        String confirmedAt = registration.getConfirmedAt() != null
                ? registration.getConfirmedAt().toString()
                : null;

        String cancelledAt = registration.getCancelledAt() != null
                ? registration.getCancelledAt().toString()
                : null;

        String status = registration.getStatus() != null
                ? registration.getStatus().name()
                : null;

        String eventTitle = registration.getEvent() != null
                ? registration.getEvent().getTitle()
                : null;

        String eventStartDateTime = registration.getEvent() != null
                ? registration.getEvent().getStartDateTime().toString()
                : null;

        String eventEndDateTime = registration.getEvent() != null
                ? (registration.getEvent().getEndDateTime() != null
                ? registration.getEvent().getEndDateTime().toString()
                : null)
                : null;

        return new RegistrationResponseModel(
                registration.getId(),
                registration.getUser().getId(),
                registration.getEvent().getId(),
                eventTitle,
                status,
                requestedAt,
                confirmedAt,
                cancelledAt,
                registration.getWaitlistedPosition(),
                eventStartDateTime,
                eventEndDateTime
        );
    }
}
