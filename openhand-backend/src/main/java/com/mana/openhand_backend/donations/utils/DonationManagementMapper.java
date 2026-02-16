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
        String donorName = donation.getDonorName() != null ? donation.getDonorName() : (user != null ? user.getName() : null);
        String donorEmail = donation.getDonorEmail() != null ? donation.getDonorEmail() : (user != null ? user.getEmail() : null);

        Long eventId = (donation.getEvent() != null) ? donation.getEvent().getId() : null;
        return new DonationSummaryResponseModel(
            donation.getId(),
            user != null ? user.getId() : null,
            eventId,
            donorName,
            donorEmail,
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
        String donorName = donation.getDonorName() != null ? donation.getDonorName() : (user != null ? user.getName() : null);
        String donorEmail = donation.getDonorEmail() != null ? donation.getDonorEmail() : (user != null ? user.getEmail() : null);
        String donorPhone = user != null ? user.getPhoneNumber() : null;

        String eventName = (donation.getEvent() != null && donation.getEvent().getTitle() != null)
            ? donation.getEvent().getTitle() : null;
        return new DonationDetailResponseModel(
            donation.getId(),
            user != null ? user.getId() : null,
            donorName,
            donorEmail,
            donorPhone,
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
