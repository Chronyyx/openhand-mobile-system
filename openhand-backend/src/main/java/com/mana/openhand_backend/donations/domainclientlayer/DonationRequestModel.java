package com.mana.openhand_backend.donations.domainclientlayer;

import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DonationRequestModel {

    @NotNull
    @DecimalMin(value = "1.00")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private DonationFrequency frequency;

    public DonationRequestModel() {
    }

    public DonationRequestModel(BigDecimal amount, String currency, DonationFrequency frequency) {
        this.amount = amount;
        this.currency = currency;
        this.frequency = frequency;
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

    public DonationFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(DonationFrequency frequency) {
        this.frequency = frequency;
    }
}
