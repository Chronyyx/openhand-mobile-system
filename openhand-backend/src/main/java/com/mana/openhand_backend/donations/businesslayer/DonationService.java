package com.mana.openhand_backend.donations.businesslayer;

import com.mana.openhand_backend.donations.domainclientlayer.DonationOptionsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;

public interface DonationService {

    DonationOptionsResponseModel getDonationOptions();

    DonationResponseModel createDonation(Long userId, DonationRequestModel request);
}
