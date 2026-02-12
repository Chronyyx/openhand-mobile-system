package com.mana.openhand_backend.donations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationRepository;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@SuppressWarnings("null")
class DonationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("member@example.com");
        user.setPasswordHash("hashedPassword");
        user = userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void getDonationOptions_returnsDefaults() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/api/donations/options")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.minimumAmount").value(1.00))
                .andExpect(jsonPath("$.presetAmounts", hasSize(4)))
                .andExpect(jsonPath("$.frequencies", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    @Transactional
    void submitDonation_withValidRequest_createsDonation() throws Exception {
        // Arrange
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "CAD", DonationFrequency.ONE_TIME);

        // Act & Assert
        mockMvc.perform(post("/api/donations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.frequency").value("ONE_TIME"));

        List<Donation> donations = donationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertEquals(1, donations.size());
        assertEquals(new BigDecimal("10.00"), donations.get(0).getAmount());
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void submitDonation_whenMemberInactive_returnsForbidden() throws Exception {
        // Arrange
        user.setMemberStatus(MemberStatus.INACTIVE);
        userRepository.save(user);
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "CAD", DonationFrequency.ONE_TIME);

        // Act & Assert
        mockMvc.perform(post("/api/donations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
