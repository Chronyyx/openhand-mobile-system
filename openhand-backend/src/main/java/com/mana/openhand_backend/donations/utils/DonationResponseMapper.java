package com.mana.openhand_backend.donations.utils;

import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;
import java.time.format.DateTimeFormatter;

public class DonationResponseMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static DonationResponseModel toResponseModel(Donation donation, String message) {
        if (donation == null) {
            return null;
        }

        String createdAt = donation.getCreatedAt() != null ? donation.getCreatedAt().format(FORMATTER) : null;
        String frequency = donation.getFrequency() != null ? donation.getFrequency().name() : null;
        String status = donation.getStatus() != null ? donation.getStatus().name() : null;

        return new DonationResponseModel(
                donation.getId(),
                donation.getAmount(),
                donation.getCurrency(),
                frequency,
                status,
                createdAt,
                message);
    }
}
