package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;

public class DonationTopDonorResponseModel {

    private Long userId;
    private String donorName;
    private String donorEmail;
    private long donationCount;
    private BigDecimal totalAmount;

    public DonationTopDonorResponseModel() {
    }

    public DonationTopDonorResponseModel(Long userId, String donorName, String donorEmail, long donationCount,
            BigDecimal totalAmount) {
        this.userId = userId;
        this.donorName = donorName;
        this.donorEmail = donorEmail;
        this.donationCount = donationCount;
        this.totalAmount = totalAmount;
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

    public long getDonationCount() {
        return donationCount;
    }

    public void setDonationCount(long donationCount) {
        this.donationCount = donationCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
