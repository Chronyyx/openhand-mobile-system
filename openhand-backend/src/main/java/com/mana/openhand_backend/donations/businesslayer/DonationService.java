package com.mana.openhand_backend.donations.businesslayer;

import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationOptionsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.ManualDonationRequestModel;
import java.util.List;

public interface DonationService {

    DonationOptionsResponseModel getDonationOptions();

    DonationResponseModel createDonation(Long userId, DonationRequestModel request);

    List<DonationSummaryResponseModel> getDonationsForStaff();

    List<DonationSummaryResponseModel> getDonationsForStaffFilteredFlexible(Long eventId, String campaignName, Integer year, Integer month, Integer day);

    DonationDetailResponseModel getDonationDetail(Long donationId);

    DonationSummaryResponseModel createManualDonation(Long employeeId, Long donorUserId, ManualDonationRequestModel request);
}
