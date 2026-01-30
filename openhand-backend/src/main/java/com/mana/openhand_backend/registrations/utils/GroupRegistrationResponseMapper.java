package com.mana.openhand_backend.registrations.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.ParticipantResponseModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GroupRegistrationResponseMapper {
    private GroupRegistrationResponseMapper() {
    }

    public static GroupRegistrationResponseModel toResponse(Event event, List<Registration> registrations) {
        List<ParticipantResponseModel> participants = toParticipants(registrations);

        ParticipantResponseModel primary = participants.stream()
                .filter(p -> p.getPrimaryRegistrant() != null && p.getPrimaryRegistrant())
                .findFirst()
                .orElse(participants.isEmpty() ? null : participants.get(0));

        Integer remainingCapacity = null;
        if (event != null && event.getMaxCapacity() != null) {
            int current = event.getCurrentRegistrations() != null ? event.getCurrentRegistrations() : 0;
            remainingCapacity = Math.max(0, event.getMaxCapacity() - current);
        }

        return new GroupRegistrationResponseModel(
                event != null ? event.getId() : null,
                primary,
                participants,
                remainingCapacity
        );
    }

    public static List<ParticipantResponseModel> toParticipants(List<Registration> registrations) {
        List<ParticipantResponseModel> participants = new ArrayList<>();
        if (registrations == null) {
            return participants;
        }
        for (Registration registration : registrations) {
            participants.add(toParticipant(registration));
        }

        participants.sort(Comparator.comparing(p -> p.getPrimaryRegistrant() != null && p.getPrimaryRegistrant() ? 0 : 1));
        return participants;
    }

    public static ParticipantResponseModel toParticipant(Registration registration) {
        User user = registration.getUser();
        String fullName = user != null ? user.getName() : registration.getParticipantFullName();
        Integer age = user != null ? user.getAge() : registration.getParticipantAge();
        String dateOfBirth = registration.getParticipantDateOfBirth() != null
                ? registration.getParticipantDateOfBirth().toString()
                : null;
        Boolean primary = registration.getPrimaryRegistrant();
        if (primary == null && user != null) {
            primary = true;
        }

        return new ParticipantResponseModel(
                registration.getId(),
                fullName,
                age,
                dateOfBirth,
                registration.getParticipantRelation(),
                primary,
                registration.getStatus() != null ? registration.getStatus().name() : null,
                registration.getWaitlistedPosition()
        );
    }
}
