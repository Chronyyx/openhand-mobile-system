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
        @GetMapping
        public java.util.List<com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel> getDonationsForAdmin(
            @org.springframework.web.bind.annotation.RequestParam(value = "eventId", required = false) Long eventId,
            @org.springframework.web.bind.annotation.RequestParam(value = "campaignName", required = false) String campaignName,
            @org.springframework.web.bind.annotation.RequestParam(value = "year", required = false) Integer year,
            @org.springframework.web.bind.annotation.RequestParam(value = "month", required = false) Integer month,
            @org.springframework.web.bind.annotation.RequestParam(value = "day", required = false) Integer day) {
        // Validate year/month/day
        if (month != null && (month < 1 || month > 12)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Invalid month. Must be between 1 and 12."
            );
        }
        if (day != null && (month == null || year == null)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Day filter requires both year and month."
            );
        }
        if (day != null && month != null && year != null) {
            try {
                java.time.LocalDate.of(year, month, day);
            } catch (Exception e) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid day for the given month/year."
                );
            }
        }
        return donationService.getDonationsForStaffFilteredFlexible(eventId, campaignName, year, month, day);
    }

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
