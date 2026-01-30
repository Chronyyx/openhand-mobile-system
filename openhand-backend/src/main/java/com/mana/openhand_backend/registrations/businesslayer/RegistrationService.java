package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.domainclientlayer.FamilyMemberRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryFilter;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel;

import java.util.List;

public interface RegistrationService {

    Registration registerForEvent(Long userId, Long eventId);

    GroupRegistrationResponseModel registerForEventWithFamily(Long userId, Long eventId,
            List<FamilyMemberRequestModel> familyMembers);

    Registration getRegistrationById(Long id);

    List<Registration> getUserRegistrations(Long userId);

    List<RegistrationHistoryResponseModel> getUserRegistrationHistory(Long userId, RegistrationHistoryFilter filter);

    Registration cancelRegistration(Long userId, Long eventId);
}
