package com.mana.openhand_backend.events.presentationlayer.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CreateEventRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String startDateTime;

    private String endDateTime;

    @NotBlank
    private String locationName;

    @NotBlank
    private String address;

    @Positive
    private Integer maxCapacity;

    private String category;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getParsedStartDateTime() {
        return parseLocalDateTime(startDateTime, "startDateTime");
    }

    public LocalDateTime getParsedEndDateTime() {
        if (endDateTime == null || endDateTime.isBlank()) {
            return null;
        }
        return parseLocalDateTime(endDateTime, "endDateTime");
    }

    private static LocalDateTime parseLocalDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim().replace(' ', 'T');

        // Support both "yyyy-MM-ddTHH:mm" and full ISO "yyyy-MM-ddTHH:mm:ss[.SSS]"
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must be an ISO local datetime like 2025-01-31T13:45");
        }
    }
}

