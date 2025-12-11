package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;

import java.util.List;

public interface EventService {

    List<Event> getUpcomingEvents();

    Event getEventById(Long id);

    RegistrationSummaryResponseModel getRegistrationSummary(Long eventId);
}
