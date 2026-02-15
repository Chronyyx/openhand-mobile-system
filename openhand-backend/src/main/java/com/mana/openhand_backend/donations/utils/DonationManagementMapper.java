package com.mana.openhand_backend.donations.utils;

import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import java.time.format.DateTimeFormatter;

public class DonationManagementMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static DonationSummaryResponseModel toSummary(Donation donation) {
        if (donation == null) {
            return null;
        }

        User user = donation.getUser();
        String createdAt = donation.getCreatedAt() != null ? donation.getCreatedAt().format(FORMATTER) : null;
        String frequency = donation.getFrequency() != null ? donation.getFrequency().name() : null;
        String status = donation.getStatus() != null ? donation.getStatus().name() : null;

        Long eventId = (donation.getEvent() != null) ? donation.getEvent().getId() : null;
        return new DonationSummaryResponseModel(
            donation.getId(),
            user != null ? user.getId() : null,
            eventId,
            user != null ? user.getName() : null,
            user != null ? user.getEmail() : null,
            donation.getAmount(),
            donation.getCurrency(),
            frequency,
            status,
            createdAt
        );
    }

    public static DonationDetailResponseModel toDetail(Donation donation) {
        if (donation == null) {
            return null;
        }

        User user = donation.getUser();
        String createdAt = donation.getCreatedAt() != null ? donation.getCreatedAt().format(FORMATTER) : null;
        String frequency = donation.getFrequency() != null ? donation.getFrequency().name() : null;
        String status = donation.getStatus() != null ? donation.getStatus().name() : null;

        String eventName = (donation.getEvent() != null && donation.getEvent().getTitle() != null)
            ? donation.getEvent().getTitle() : null;
        return new DonationDetailResponseModel(
            donation.getId(),
            user != null ? user.getId() : null,
            user != null ? user.getName() : null,
            user != null ? user.getEmail() : null,
            user != null ? user.getPhoneNumber() : null,
            donation.getAmount(),
            donation.getCurrency(),
            frequency,
            status,
            createdAt,
            donation.getPaymentProvider(),
            donation.getPaymentReference(),
            eventName
        );
    }
}
