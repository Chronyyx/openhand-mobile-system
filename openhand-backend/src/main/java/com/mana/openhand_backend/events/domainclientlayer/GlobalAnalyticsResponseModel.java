package com.mana.openhand_backend.events.domainclientlayer;

import java.util.List;

public class GlobalAnalyticsResponseModel {

    private double totalWaitlistedPercentage;
    private int totalConfirmed;
    private int totalWaitlisted;
    private double currentGlobalVelocity;
    private double historicalGlobalVelocity;
    private double velocityDeltaPercentage;
    private boolean performingBetterThanUsual;

    private List<EventPerformanceSummary> activeEventPerformances;

    public GlobalAnalyticsResponseModel() {
    }

    public double getTotalWaitlistedPercentage() {
        return totalWaitlistedPercentage;
    }

    public void setTotalWaitlistedPercentage(double totalWaitlistedPercentage) {
        this.totalWaitlistedPercentage = totalWaitlistedPercentage;
    }

    public int getTotalConfirmed() {
        return totalConfirmed;
    }

    public void setTotalConfirmed(int totalConfirmed) {
        this.totalConfirmed = totalConfirmed;
    }

    public int getTotalWaitlisted() {
        return totalWaitlisted;
    }

    public void setTotalWaitlisted(int totalWaitlisted) {
        this.totalWaitlisted = totalWaitlisted;
    }

    public double getCurrentGlobalVelocity() {
        return currentGlobalVelocity;
    }

    public void setCurrentGlobalVelocity(double currentGlobalVelocity) {
        this.currentGlobalVelocity = currentGlobalVelocity;
    }

    public double getHistoricalGlobalVelocity() {
        return historicalGlobalVelocity;
    }

    public void setHistoricalGlobalVelocity(double historicalGlobalVelocity) {
        this.historicalGlobalVelocity = historicalGlobalVelocity;
    }

    public double getVelocityDeltaPercentage() {
        return velocityDeltaPercentage;
    }

    public void setVelocityDeltaPercentage(double velocityDeltaPercentage) {
        this.velocityDeltaPercentage = velocityDeltaPercentage;
    }

    public boolean isPerformingBetterThanUsual() {
        return performingBetterThanUsual;
    }

    public void setPerformingBetterThanUsual(boolean performingBetterThanUsual) {
        this.performingBetterThanUsual = performingBetterThanUsual;
    }

    public List<EventPerformanceSummary> getActiveEventPerformances() {
        return activeEventPerformances;
    }

    public void setActiveEventPerformances(List<EventPerformanceSummary> activeEventPerformances) {
        this.activeEventPerformances = activeEventPerformances;
    }

    public static class EventPerformanceSummary {
        private Long eventId;
        private String title;
        private int currentRegistrations;
        private Integer maxCapacity;
        private double attendanceDeltaVsNorm;
        private boolean trendingUp;

        public EventPerformanceSummary(Long eventId, String title, int currentRegistrations, Integer maxCapacity,
                double attendanceDeltaVsNorm, boolean trendingUp) {
            this.eventId = eventId;
            this.title = title;
            this.currentRegistrations = currentRegistrations;
            this.maxCapacity = maxCapacity;
            this.attendanceDeltaVsNorm = attendanceDeltaVsNorm;
            this.trendingUp = trendingUp;
        }

        public Long getEventId() {
            return eventId;
        }

        public String getTitle() {
            return title;
        }

        public int getCurrentRegistrations() {
            return currentRegistrations;
        }

        public Integer getMaxCapacity() {
            return maxCapacity;
        }

        public double getAttendanceDeltaVsNorm() {
            return attendanceDeltaVsNorm;
        }

        public boolean isTrendingUp() {
            return trendingUp;
        }
    }
}
