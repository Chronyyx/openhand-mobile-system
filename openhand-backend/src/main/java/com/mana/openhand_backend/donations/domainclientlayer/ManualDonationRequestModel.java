package com.mana.openhand_backend.donations.domainclientlayer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ManualDonationRequestModel {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private String currency;

    private Long eventId;

    private LocalDateTime donationDate;

    private String comments;

    public ManualDonationRequestModel() {
    }

    public ManualDonationRequestModel(BigDecimal amount, String currency, Long eventId,
                                       LocalDateTime donationDate, String comments) {
        this.amount = amount;
        this.currency = currency;
        this.eventId = eventId;
        this.donationDate = donationDate;
        this.comments = comments;
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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDateTime donationDate) {
        this.donationDate = donationDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
