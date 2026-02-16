package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricBreakdownResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMonthlyTrendResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationTopDonorResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DonationAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DonationService donationService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getDonationMetrics_returnsSummary() throws Exception {
        // Arrange
        DonationMetricsResponseModel metrics = new DonationMetricsResponseModel(
                "CAD",
                12L,
                new BigDecimal("560.00"),
                new BigDecimal("46.67"),
                7L,
                3L,
                4L,
                List.of(new DonationMetricBreakdownResponseModel("ONE_TIME", 8L, new BigDecimal("410.00"))),
                List.of(new DonationMetricBreakdownResponseModel("RECEIVED", 11L, new BigDecimal("550.00"))),
                List.of(new DonationMonthlyTrendResponseModel("2026-02", 3L, new BigDecimal("120.00"))),
                List.of(new DonationTopDonorResponseModel(11L, "Ada", "ada@mana.org", 4L, new BigDecimal("200.00"))),
                List.of(new DonationTopDonorResponseModel(11L, "Ada", "ada@mana.org", 4L, new BigDecimal("200.00"))),
                5L,
                new BigDecimal("250.00"),
                7L,
                new BigDecimal("310.00"),
                2L,
                16.67d,
                20L,
                12L,
                8L
        );
        when(donationService.getDonationMetrics()).thenReturn(metrics);

        // Act & Assert
        mockMvc.perform(get("/api/admin/donations/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.totalDonations").value(12))
                .andExpect(jsonPath("$.manualDonationsCount").value(5))
                .andExpect(jsonPath("$.donationNotificationsUnread").value(8));
    }

    @Test
    void getDonationDetail_returnsDetail() throws Exception {
        // Arrange
        DonationDetailResponseModel detail = new DonationDetailResponseModel(
            5L,
            11L,
            "Sam Rivera",
            "sam@mana.org",
            "+15141234567",
            new BigDecimal("25.00"),
            "CAD",
            "MONTHLY",
            "RECEIVED",
            "2025-02-01T12:00:00",
            "Zeffy",
            "PAY-123",
            "Spring Gala"
        );
        when(donationService.getDonationDetail(5L)).thenReturn(detail);

        // Act & Assert
        mockMvc.perform(get("/api/admin/donations/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.paymentProvider").value("Zeffy"));
    }

    @Test
    void getDonationReport_returnsRowsInDateRange() throws Exception {
        List<DonationSummaryResponseModel> rows = List.of(
                new DonationSummaryResponseModel(
                        10L,
                        22L,
                        101L,
                        "Ari Doe",
                        "ari@mana.org",
                        new BigDecimal("40.00"),
                        "CAD",
                        "ONE_TIME",
                        "RECEIVED",
                        "2025-03-12T09:00:00"));

        when(donationService.getDonationReportByDateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)))
                .thenReturn(rows);

        mockMvc.perform(get("/api/admin/donations/reports?startDate=2025-03-01&endDate=2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].amount").value(40.00))
                .andExpect(jsonPath("$[0].currency").value("CAD"));
    }

    @Test
    void exportDonationReport_returnsCsvFile() throws Exception {
        List<DonationSummaryResponseModel> rows = List.of(
                new DonationSummaryResponseModel(
                        7L,
                        90L,
                        null,
                        "Mina Patel",
                        "mina@mana.org",
                        new BigDecimal("20.00"),
                        "CAD",
                        "ONE_TIME",
                        "RECEIVED",
                        "2025-04-01T11:30:00"));

        when(donationService.getDonationReportByDateRange(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30)))
                .thenReturn(rows);

        mockMvc.perform(get("/api/admin/donations/reports/export?startDate=2025-04-01&endDate=2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"donation-report.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Donation ID,Donor Name,Donor Email")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mina Patel")));
    }

    @Test
    void getDonationReport_withInvalidDateRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/donations/reports?startDate=2025-05-15&endDate=2025-05-01"))
                .andExpect(status().isBadRequest());
    }
}
