package com.mana.openhand_backend.events.domainclientlayer;

import java.util.List;

public class EventAnalyticsResponseModel {

    private Long eventId;
    private String title;
    private String category;

    // Stage 6: Extracted performance metrics (Delta percentages)
    private Double confirmedDeltaVsUsual;
    private Double waitlistDeltaVsUsual;
    private Double currentVelocity; // registrations per day
    private Integer predictedFinalAttendance;
    private Integer estimatedDaysToFill; // days until event reaches capacity at current velocity

    // Stage 1 & 2: Reconstructed and Normalized Timeline for this specific event
    private List<DailyMetric> eventTimeline;

    // Stage 3: Aggregated "Usual" Timeline for comparison
    private List<DailyMetric> usualTrendTimeline;

    public EventAnalyticsResponseModel() {
    }

    public static class DailyMetric {
        private int daysBeforeEvent;
        private String date;
        private int confirmed;
        private int waitlisted;
        private int cancelled;

        public DailyMetric() {
        }

        public DailyMetric(int daysBeforeEvent, String date, int confirmed, int waitlisted, int cancelled) {
            this.daysBeforeEvent = daysBeforeEvent;
            this.date = date;
            this.confirmed = confirmed;
            this.waitlisted = waitlisted;
            this.cancelled = cancelled;
        }

        public int getDaysBeforeEvent() {
            return daysBeforeEvent;
        }

        public void setDaysBeforeEvent(int daysBeforeEvent) {
            this.daysBeforeEvent = daysBeforeEvent;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getConfirmed() {
            return confirmed;
        }

        public void setConfirmed(int confirmed) {
            this.confirmed = confirmed;
        }

        public int getWaitlisted() {
            return waitlisted;
        }

        public void setWaitlisted(int waitlisted) {
            this.waitlisted = waitlisted;
        }

        public int getCancelled() {
            return cancelled;
        }

        public void setCancelled(int cancelled) {
            this.cancelled = cancelled;
        }
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getConfirmedDeltaVsUsual() {
        return confirmedDeltaVsUsual;
    }

    public void setConfirmedDeltaVsUsual(Double confirmedDeltaVsUsual) {
        this.confirmedDeltaVsUsual = confirmedDeltaVsUsual;
    }

    public Double getWaitlistDeltaVsUsual() {
        return waitlistDeltaVsUsual;
    }

    public void setWaitlistDeltaVsUsual(Double waitlistDeltaVsUsual) {
        this.waitlistDeltaVsUsual = waitlistDeltaVsUsual;
    }

    public Double getCurrentVelocity() {
        return currentVelocity;
    }

    public void setCurrentVelocity(Double currentVelocity) {
        this.currentVelocity = currentVelocity;
    }

    public Integer getPredictedFinalAttendance() {
        return predictedFinalAttendance;
    }

    public void setPredictedFinalAttendance(Integer predictedFinalAttendance) {
        this.predictedFinalAttendance = predictedFinalAttendance;
    }

    public Integer getEstimatedDaysToFill() {
        return estimatedDaysToFill;
    }

    public void setEstimatedDaysToFill(Integer estimatedDaysToFill) {
        this.estimatedDaysToFill = estimatedDaysToFill;
    }

    public List<DailyMetric> getEventTimeline() {
        return eventTimeline;
    }

    public void setEventTimeline(List<DailyMetric> eventTimeline) {
        this.eventTimeline = eventTimeline;
    }

    public List<DailyMetric> getUsualTrendTimeline() {
        return usualTrendTimeline;
    }

    public void setUsualTrendTimeline(List<DailyMetric> usualTrendTimeline) {
        this.usualTrendTimeline = usualTrendTimeline;
    }
}
