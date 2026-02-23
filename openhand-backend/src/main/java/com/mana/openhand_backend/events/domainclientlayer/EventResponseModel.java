package com.mana.openhand_backend.events.domainclientlayer;

public class EventResponseModel {

    private Long id;
    private String title;
    private String description;
    private String startDateTime;
    private String endDateTime;
    private String locationName;
    private String address;
    private String status;
    private Integer maxCapacity;
    private Integer currentRegistrations;
    private String category;
    private String imageUrl;

    private Integer totalRegistrations;
    private Integer totalUnregistrations;
    private Integer totalWaitlistCount;
    private Integer finalWaitlistCount;
    private String createdAt;
    private String completedAt;

    public EventResponseModel() {
    }

    public EventResponseModel(Long id,
            String title,
            String description,
            String startDateTime,
            String endDateTime,
            String locationName,
            String address,
            String status,
            Integer maxCapacity,
            Integer currentRegistrations,
            String category,
            String imageUrl,
            Integer totalRegistrations,
            Integer totalUnregistrations,
            Integer totalWaitlistCount,
            Integer finalWaitlistCount,
            String createdAt,
            String completedAt) {
        this.id = id;
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
        this.imageUrl = imageUrl;
        this.totalRegistrations = totalRegistrations;
        this.totalUnregistrations = totalUnregistrations;
        this.totalWaitlistCount = totalWaitlistCount;
        this.finalWaitlistCount = finalWaitlistCount;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
