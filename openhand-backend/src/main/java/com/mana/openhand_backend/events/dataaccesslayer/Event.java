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

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private Integer maxCapacity;
    private Integer currentRegistrations;

    @Column(name = "category")
    private String category;

    @Column(name = "total_registrations")
    private Integer totalRegistrations = 0;

    @Column(name = "total_unregistrations")
    private Integer totalUnregistrations = 0;

    @Column(name = "total_waitlist_count")
    private Integer totalWaitlistCount = 0;

    @Column(name = "final_waitlist_count")
    private Integer finalWaitlistCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(name = "version")
    private Long version;

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
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(Integer totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public Integer getTotalUnregistrations() {
        return totalUnregistrations;
    }

    public void setTotalUnregistrations(Integer totalUnregistrations) {
        this.totalUnregistrations = totalUnregistrations;
    }

    public Integer getTotalWaitlistCount() {
        return totalWaitlistCount;
    }

    public void setTotalWaitlistCount(Integer totalWaitlistCount) {
        this.totalWaitlistCount = totalWaitlistCount;
    }

    public Integer getFinalWaitlistCount() {
        return finalWaitlistCount;
    }

    public void setFinalWaitlistCount(Integer finalWaitlistCount) {
        this.finalWaitlistCount = finalWaitlistCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
