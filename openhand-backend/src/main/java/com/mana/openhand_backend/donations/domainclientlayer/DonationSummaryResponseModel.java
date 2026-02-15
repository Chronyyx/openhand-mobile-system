package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;


import com.fasterxml.jackson.annotation.JsonProperty;

public class DonationSummaryResponseModel {
    @JsonProperty("id")
    private Long id;
    private Long userId;
    @JsonProperty("eventId")
    private Long eventId;
    private String donorName;
    private String donorEmail;
    private BigDecimal amount;
    private String currency;
    private String frequency;
    private String status;
    private String createdAt;


    public DonationSummaryResponseModel() {
    }

    public DonationSummaryResponseModel(Long id, Long userId, Long eventId, String donorName, String donorEmail,
            BigDecimal amount, String currency, String frequency, String status, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.donorName = donorName;
        this.donorEmail = donorEmail;
        this.amount = amount;
        this.currency = currency;
        this.frequency = frequency;
        this.status = status;
        this.createdAt = createdAt;
    }

    @JsonProperty("eventId")
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }


    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
