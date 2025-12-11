package com.mana.openhand_backend.events.dataaccesslayer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private String locationName;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private Integer maxCapacity;
    private Integer currentRegistrations;

    @Column(name = "category")
    private String category;

    protected Event() {
    }

    public Event(
            String title,
            String description,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String locationName,
            String address,
            EventStatus status,
            Integer maxCapacity,
            Integer currentRegistrations,
            String category) {
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.locationName = locationName;
        this.address = address;
        this.status = status;
        this.maxCapacity = maxCapacity;
        this.currentRegistrations = currentRegistrations;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

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

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
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

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getCurrentRegistrations() {
        return currentRegistrations;
    }

    public void setCurrentRegistrations(Integer currentRegistrations) {
        this.currentRegistrations = currentRegistrations;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
