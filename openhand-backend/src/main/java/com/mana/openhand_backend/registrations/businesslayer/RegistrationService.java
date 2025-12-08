package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;

import java.util.List;

public interface RegistrationService {

    Registration registerForEvent(Long userId, Long eventId);

    Registration getRegistrationById(Long id);

    List<Registration> getUserRegistrations(Long userId);

    Registration cancelRegistration(Long userId, Long eventId);
}
