package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.common.presentationlayer.payload.ImageUrlResponse;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;

import org.springframework.web.multipart.MultipartFile;

public interface EventAdminService {
        Event createEvent(CreateEventRequest request);

        Event updateEvent(Long id, CreateEventRequest request);

        Event cancelEvent(Long id);

        ImageUrlResponse uploadEventImage(Long id, MultipartFile file, String baseUrl);

        ImageUrlResponse getEventImage(Long id, String baseUrl);
}
