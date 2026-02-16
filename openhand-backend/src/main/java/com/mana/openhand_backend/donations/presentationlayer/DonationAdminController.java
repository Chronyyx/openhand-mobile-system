package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.donations.utils.DonationCsvExportUtil;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/donations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class DonationAdminController {
    @GetMapping
    public List<DonationSummaryResponseModel> getDonationsForAdmin(
            @RequestParam(value = "eventId", required = false) Long eventId,
            @RequestParam(value = "campaignName", required = false) String campaignName,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "day", required = false) Integer day) {
        validateLegacyDateFilters(year, month, day);
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

    @GetMapping("/reports")
    public List<DonationSummaryResponseModel> getDonationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return donationService.getDonationReportByDateRange(startDate, endDate);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<String> exportDonationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(startDate, endDate);
        List<DonationSummaryResponseModel> reportRows = donationService.getDonationReportByDateRange(startDate, endDate);
        String csvContent = DonationCsvExportUtil.toDonationReportCsv(reportRows);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"donation-report.csv\"")
                .body(csvContent);
    }

    private void validateLegacyDateFilters(Integer year, Integer month, Integer day) {
        if (month != null && (month < 1 || month > 12)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month. Must be between 1 and 12.");
        }
        if (day != null && (month == null || year == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Day filter requires both year and month.");
        }
        if (day != null && month != null && year != null) {
            try {
                LocalDate.of(year, month, day);
            } catch (RuntimeException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid day for the given month/year.");
            }
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
    }
}
