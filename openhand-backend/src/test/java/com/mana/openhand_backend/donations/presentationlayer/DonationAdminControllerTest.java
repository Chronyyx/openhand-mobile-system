package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricBreakdownResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMonthlyTrendResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationTopDonorResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
