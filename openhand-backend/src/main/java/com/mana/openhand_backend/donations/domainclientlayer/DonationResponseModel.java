package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;

public class DonationResponseModel {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private String frequency;
    private String status;
    private String createdAt;
    private String message;

    public DonationResponseModel() {
    }

    public DonationResponseModel(Long id, BigDecimal amount, String currency, String frequency, String status,
            String createdAt, String message) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.frequency = frequency;
        this.status = status;
        this.createdAt = createdAt;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
