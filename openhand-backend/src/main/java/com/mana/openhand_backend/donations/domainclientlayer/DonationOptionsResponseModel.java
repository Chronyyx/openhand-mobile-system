package com.mana.openhand_backend.donations.domainclientlayer;

import java.math.BigDecimal;
import java.util.List;

public class DonationOptionsResponseModel {

    private String currency;
    private BigDecimal minimumAmount;
    private List<Integer> presetAmounts;
    private List<String> frequencies;

    public DonationOptionsResponseModel() {
    }

    public DonationOptionsResponseModel(String currency, BigDecimal minimumAmount, List<Integer> presetAmounts,
            List<String> frequencies) {
        this.currency = currency;
        this.minimumAmount = minimumAmount;
        this.presetAmounts = presetAmounts;
        this.frequencies = frequencies;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public List<Integer> getPresetAmounts() {
        return presetAmounts;
    }

    public void setPresetAmounts(List<Integer> presetAmounts) {
        this.presetAmounts = presetAmounts;
    }

    public List<String> getFrequencies() {
        return frequencies;
    }

    public void setFrequencies(List<String> frequencies) {
        this.frequencies = frequencies;
    }
}
