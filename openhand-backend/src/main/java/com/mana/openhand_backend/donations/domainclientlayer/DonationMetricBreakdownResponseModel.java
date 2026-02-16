package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;

public class DonationMetricBreakdownResponseModel {

    private String key;
    private long count;
    private BigDecimal amount;

    public DonationMetricBreakdownResponseModel() {
    }

    public DonationMetricBreakdownResponseModel(String key, long count, BigDecimal amount) {
        this.key = key;
        this.count = count;
        this.amount = amount;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
