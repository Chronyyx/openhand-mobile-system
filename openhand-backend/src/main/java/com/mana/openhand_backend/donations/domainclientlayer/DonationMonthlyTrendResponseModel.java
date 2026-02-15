package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;

public class DonationMonthlyTrendResponseModel {

    private String period;
    private long count;
    private BigDecimal amount;

    public DonationMonthlyTrendResponseModel() {
    }

    public DonationMonthlyTrendResponseModel(String period, long count, BigDecimal amount) {
        this.period = period;
        this.count = count;
        this.amount = amount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
