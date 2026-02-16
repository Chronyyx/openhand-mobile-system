package com.mana.openhand_backend.donations.domainclientlayer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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

    private Long donorUserId;

    private String donorName;

    @Email(message = "Donor email must be a valid email address")
    private String donorEmail;

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

    public ManualDonationRequestModel(BigDecimal amount, String currency, Long eventId,
                                      LocalDateTime donationDate, String comments,
                                      Long donorUserId, String donorName, String donorEmail) {
        this.amount = amount;
        this.currency = currency;
        this.eventId = eventId;
        this.donationDate = donationDate;
        this.comments = comments;
        this.donorUserId = donorUserId;
        this.donorName = donorName;
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

    public Long getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(Long donorUserId) {
        this.donorUserId = donorUserId;
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
}
