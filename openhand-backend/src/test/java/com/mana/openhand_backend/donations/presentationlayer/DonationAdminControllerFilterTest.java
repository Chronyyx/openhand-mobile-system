package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DonationAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonationAdminControllerFilterTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DonationService donationService;
    @MockBean
    private UserRepository userRepository;

    @Test
    void getDonationsForAdmin_filtersByEventId() throws Exception {
        // Arrange
        DonationSummaryResponseModel summary = new DonationSummaryResponseModel(
                1L, 10L, 100L, "Alex Doe", "alex@mana.org", new BigDecimal("15.00"), "CAD", "ONE_TIME", "RECEIVED", "2024-05-10T12:00:00");
        when(donationService.getDonationsForStaffFilteredFlexible(eq(100L), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(summary));
        // Act & Assert
        mockMvc.perform(get("/api/admin/donations?eventId=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventId").value(100));
    }

    @Test
    void getDonationsForAdmin_filtersByCampaignName() throws Exception {
        // Arrange
        DonationSummaryResponseModel summary = new DonationSummaryResponseModel(
                2L, 20L, 101L, "Jane Smith", "jane@mana.org", new BigDecimal("20.00"), "CAD", "ONE_TIME", "RECEIVED", "2024-06-15T10:00:00");
        when(donationService.getDonationsForStaffFilteredFlexible(isNull(), anyString(), isNull(), isNull(), isNull())).thenReturn(List.of(summary));
        // Act & Assert
        mockMvc.perform(get("/api/admin/donations?campaignName=Summer%20Fundraiser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].eventId").value(101));
    }

    @Test
    void getDonationsForAdmin_filtersByYearMonthDay() throws Exception {
        // Arrange
        DonationSummaryResponseModel summary = new DonationSummaryResponseModel(
                3L, 30L, 102L, "Sam Lee", "sam@mana.org", new BigDecimal("30.00"), "CAD", "ONE_TIME", "RECEIVED", "2023-09-05T08:00:00");
        when(donationService.getDonationsForStaffFilteredFlexible(isNull(), isNull(), eq(2023), eq(9), eq(5))).thenReturn(List.of(summary));
        // Act & Assert
        mockMvc.perform(get("/api/admin/donations?year=2023&month=9&day=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].eventId").value(102));
    }

    @Test
    void getDonationsForAdmin_filtersByYearAndMonth() throws Exception {
        // Arrange
        DonationSummaryResponseModel summary1 = new DonationSummaryResponseModel(
                4L, 40L, 103L, "Chris Kim", "chris@mana.org", new BigDecimal("40.00"), "CAD", "ONE_TIME", "RECEIVED", "2022-12-01T08:00:00");
        DonationSummaryResponseModel summary2 = new DonationSummaryResponseModel(
                5L, 50L, 103L, "Pat Lee", "pat@mana.org", new BigDecimal("50.00"), "CAD", "ONE_TIME", "RECEIVED", "2022-12-31T08:00:00");
        when(donationService.getDonationsForStaffFilteredFlexible(isNull(), isNull(), eq(2022), eq(12), isNull())).thenReturn(List.of(summary1, summary2));
        // Act & Assert
        mockMvc.perform(get("/api/admin/donations?year=2022&month=12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[1].id").value(5));
    }

    @Test
    void getDonationsForAdmin_filtersByYear() throws Exception {
        // Arrange
        DonationSummaryResponseModel summary1 = new DonationSummaryResponseModel(
                6L, 60L, 104L, "Taylor Ray", "taylor@mana.org", new BigDecimal("60.00"), "CAD", "ONE_TIME", "RECEIVED", "2021-01-01T08:00:00");
        DonationSummaryResponseModel summary2 = new DonationSummaryResponseModel(
                7L, 70L, 104L, "Morgan Lee", "morgan@mana.org", new BigDecimal("70.00"), "CAD", "ONE_TIME", "RECEIVED", "2021-12-31T08:00:00");
        when(donationService.getDonationsForStaffFilteredFlexible(isNull(), isNull(), eq(2021), isNull(), isNull())).thenReturn(List.of(summary1, summary2));
        // Act & Assert
        mockMvc.perform(get("/api/admin/donations?year=2021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6))
                .andExpect(jsonPath("$[1].id").value(7));
    }

    @Test
    void getDonationsForAdmin_withNoParams_returnsAll() throws Exception {
        // Arrange
        DonationSummaryResponseModel summary = new DonationSummaryResponseModel(
                8L, 80L, 105L, "Jordan Fox", "jordan@mana.org", new BigDecimal("80.00"), "CAD", "ONE_TIME", "RECEIVED", "2020-07-20T08:00:00");
        when(donationService.getDonationsForStaffFilteredFlexible(isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(summary));
        // Act & Assert
        mockMvc.perform(get("/api/admin/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].eventId").value(105));
    }
}
