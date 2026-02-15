package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.common.presentationlayer.payload.ImageUrlResponse;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface EventAdminService {
                /**
                 * Returns all events (for admin dropdowns, etc).
                 */
                List<Event> getAllEvents();
        Event createEvent(CreateEventRequest request);

        Event updateEvent(Long id, CreateEventRequest request);

        Event cancelEvent(Long id);

        ImageUrlResponse uploadEventImage(Long id, MultipartFile file, String baseUrl);

        ImageUrlResponse getEventImage(Long id, String baseUrl);
}
