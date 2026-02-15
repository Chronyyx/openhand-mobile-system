package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;
import java.util.List;

public class DonationMetricsResponseModel {

    private String currency;
    private long totalDonations;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
    private long uniqueDonors;
    private long repeatDonors;
    private long firstTimeDonors;
    private List<DonationMetricBreakdownResponseModel> frequencyBreakdown;
    private List<DonationMetricBreakdownResponseModel> statusBreakdown;
    private List<DonationMonthlyTrendResponseModel> monthlyTrend;
    private List<DonationTopDonorResponseModel> topDonorsByAmount;
    private List<DonationTopDonorResponseModel> topDonorsByCount;
    private long manualDonationsCount;
    private BigDecimal manualDonationsAmount;
    private long externalDonationsCount;
    private BigDecimal externalDonationsAmount;
    private long commentsCount;
    private double commentsUsageRate;
    private long donationNotificationsCreated;
    private long donationNotificationsRead;
    private long donationNotificationsUnread;

    public DonationMetricsResponseModel() {
    }

    public DonationMetricsResponseModel(
            String currency,
            long totalDonations,
            BigDecimal totalAmount,
            BigDecimal averageAmount,
            long uniqueDonors,
            long repeatDonors,
            long firstTimeDonors,
            List<DonationMetricBreakdownResponseModel> frequencyBreakdown,
            List<DonationMetricBreakdownResponseModel> statusBreakdown,
            List<DonationMonthlyTrendResponseModel> monthlyTrend,
            List<DonationTopDonorResponseModel> topDonorsByAmount,
            List<DonationTopDonorResponseModel> topDonorsByCount,
            long manualDonationsCount,
            BigDecimal manualDonationsAmount,
            long externalDonationsCount,
            BigDecimal externalDonationsAmount,
            long commentsCount,
            double commentsUsageRate,
            long donationNotificationsCreated,
            long donationNotificationsRead,
            long donationNotificationsUnread) {
        this.currency = currency;
        this.totalDonations = totalDonations;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
        this.uniqueDonors = uniqueDonors;
        this.repeatDonors = repeatDonors;
        this.firstTimeDonors = firstTimeDonors;
        this.frequencyBreakdown = frequencyBreakdown;
        this.statusBreakdown = statusBreakdown;
        this.monthlyTrend = monthlyTrend;
        this.topDonorsByAmount = topDonorsByAmount;
        this.topDonorsByCount = topDonorsByCount;
        this.manualDonationsCount = manualDonationsCount;
        this.manualDonationsAmount = manualDonationsAmount;
        this.externalDonationsCount = externalDonationsCount;
        this.externalDonationsAmount = externalDonationsAmount;
        this.commentsCount = commentsCount;
        this.commentsUsageRate = commentsUsageRate;
        this.donationNotificationsCreated = donationNotificationsCreated;
        this.donationNotificationsRead = donationNotificationsRead;
        this.donationNotificationsUnread = donationNotificationsUnread;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(long totalDonations) {
        this.totalDonations = totalDonations;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(BigDecimal averageAmount) {
        this.averageAmount = averageAmount;
    }

    public long getUniqueDonors() {
        return uniqueDonors;
    }

    public void setUniqueDonors(long uniqueDonors) {
        this.uniqueDonors = uniqueDonors;
    }

    public long getRepeatDonors() {
        return repeatDonors;
    }

    public void setRepeatDonors(long repeatDonors) {
        this.repeatDonors = repeatDonors;
    }

    public long getFirstTimeDonors() {
        return firstTimeDonors;
    }

    public void setFirstTimeDonors(long firstTimeDonors) {
        this.firstTimeDonors = firstTimeDonors;
    }

    public List<DonationMetricBreakdownResponseModel> getFrequencyBreakdown() {
        return frequencyBreakdown;
    }

    public void setFrequencyBreakdown(List<DonationMetricBreakdownResponseModel> frequencyBreakdown) {
        this.frequencyBreakdown = frequencyBreakdown;
    }

    public List<DonationMetricBreakdownResponseModel> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(List<DonationMetricBreakdownResponseModel> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }

    public List<DonationMonthlyTrendResponseModel> getMonthlyTrend() {
        return monthlyTrend;
    }

    public void setMonthlyTrend(List<DonationMonthlyTrendResponseModel> monthlyTrend) {
        this.monthlyTrend = monthlyTrend;
    }

    public List<DonationTopDonorResponseModel> getTopDonorsByAmount() {
        return topDonorsByAmount;
    }

    public void setTopDonorsByAmount(List<DonationTopDonorResponseModel> topDonorsByAmount) {
        this.topDonorsByAmount = topDonorsByAmount;
    }

    public List<DonationTopDonorResponseModel> getTopDonorsByCount() {
        return topDonorsByCount;
    }

    public void setTopDonorsByCount(List<DonationTopDonorResponseModel> topDonorsByCount) {
        this.topDonorsByCount = topDonorsByCount;
    }

    public long getManualDonationsCount() {
        return manualDonationsCount;
    }

    public void setManualDonationsCount(long manualDonationsCount) {
        this.manualDonationsCount = manualDonationsCount;
    }

    public BigDecimal getManualDonationsAmount() {
        return manualDonationsAmount;
    }

    public void setManualDonationsAmount(BigDecimal manualDonationsAmount) {
        this.manualDonationsAmount = manualDonationsAmount;
    }

    public long getExternalDonationsCount() {
        return externalDonationsCount;
    }

    public void setExternalDonationsCount(long externalDonationsCount) {
        this.externalDonationsCount = externalDonationsCount;
    }

    public BigDecimal getExternalDonationsAmount() {
        return externalDonationsAmount;
    }

    public void setExternalDonationsAmount(BigDecimal externalDonationsAmount) {
        this.externalDonationsAmount = externalDonationsAmount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public double getCommentsUsageRate() {
        return commentsUsageRate;
    }

    public void setCommentsUsageRate(double commentsUsageRate) {
        this.commentsUsageRate = commentsUsageRate;
    }

    public long getDonationNotificationsCreated() {
        return donationNotificationsCreated;
    }

    public void setDonationNotificationsCreated(long donationNotificationsCreated) {
        this.donationNotificationsCreated = donationNotificationsCreated;
    }

    public long getDonationNotificationsRead() {
        return donationNotificationsRead;
    }

    public void setDonationNotificationsRead(long donationNotificationsRead) {
        this.donationNotificationsRead = donationNotificationsRead;
    }

    public long getDonationNotificationsUnread() {
        return donationNotificationsUnread;
    }

    public void setDonationNotificationsUnread(long donationNotificationsUnread) {
        this.donationNotificationsUnread = donationNotificationsUnread;
    }
}
