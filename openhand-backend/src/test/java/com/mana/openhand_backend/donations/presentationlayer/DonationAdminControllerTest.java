package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = DonationAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    @Test
    void getDonationDetail_returnsDetail() throws Exception {
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
                "PAY-123"
        );
        when(donationService.getDonationDetail(5L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/donations/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.paymentProvider").value("Zeffy"));
    }
}
