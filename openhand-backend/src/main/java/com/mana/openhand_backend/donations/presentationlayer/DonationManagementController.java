package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/donations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class DonationManagementController {

    private final DonationService donationService;

    public DonationManagementController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping
    public List<DonationSummaryResponseModel> getDonationsForStaff() {
        return donationService.getDonationsForStaff();
    }
}
