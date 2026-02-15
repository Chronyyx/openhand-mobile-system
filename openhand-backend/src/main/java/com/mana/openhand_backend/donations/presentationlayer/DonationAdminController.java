package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricsResponseModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/donations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class DonationAdminController {

    private final DonationService donationService;

    public DonationAdminController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping("/metrics")
    public DonationMetricsResponseModel getDonationMetrics() {
        return donationService.getDonationMetrics();
    }

    @GetMapping("/{id}")
    public DonationDetailResponseModel getDonationDetail(@PathVariable Long id) {
        return donationService.getDonationDetail(id);
    }
}
