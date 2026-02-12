package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DonationManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonationManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    @Test
    void getDonationsForStaff_returnsList() throws Exception {
        DonationSummaryResponseModel summary = new DonationSummaryResponseModel(
                1L,
                10L,
                "Alex Doe",
                "alex@mana.org",
                new BigDecimal("15.00"),
                "CAD",
                "ONE_TIME",
                "RECEIVED",
                "2025-01-01T10:00:00"
        );
        when(donationService.getDonationsForStaff()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/employee/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].donorEmail").value("alex@mana.org"));
    }
}
