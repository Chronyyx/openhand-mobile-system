package com.mana.openhand_backend.donations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationRepository;
import com.mana.openhand_backend.donations.domainclientlayer.ManualDonationRequestModel;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import jakarta.persistence.EntityManager; 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@SuppressWarnings("null")
class DonationManualEntryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User donorUser;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        // Arrange
        donationRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        donorUser = new User();
        donorUser.setEmail("donor@example.com");
        donorUser.setPasswordHash("hashedPassword");
        donorUser.setMemberStatus(MemberStatus.ACTIVE);
        Set<String> donorRoles = new HashSet<>();
        donorRoles.add("ROLE_MEMBER");
        donorUser.setRoles(donorRoles);
        donorUser = userRepository.save(donorUser);

        employeeUser = new User();
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPasswordHash("hashedPassword");
        employeeUser.setMemberStatus(MemberStatus.ACTIVE);
        Set<String> employeeRoles = new HashSet<>();
        employeeRoles.add("ROLE_EMPLOYEE");
        employeeUser.setRoles(employeeRoles);
        employeeUser = userRepository.save(employeeUser);
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void createManualDonation_withValidRequest_persistsToDatabaseSuccessfully() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("50.00"), "CAD", null, LocalDateTime.of(2025, 1, 20, 10, 30), "Monthly supporter");

        // Act
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.currency").value("CAD"));

        // Assert
        List<Donation> donations = donationRepository.findByUserIdOrderByCreatedAtDesc(donorUser.getId());
        assertEquals(1, donations.size());
        
        Donation saved = donations.get(0);
        assertEquals(new BigDecimal("50.00"), saved.getAmount());
        assertEquals("CAD", saved.getCurrency());
        assertEquals("ONE_TIME", saved.getFrequency().toString());
        assertEquals("RECEIVED", saved.getStatus().toString());
        assertEquals("Manual Entry", saved.getPaymentProvider());
        assertTrue(saved.getPaymentReference().startsWith("MANUAL-"));
        assertEquals("Monthly supporter", saved.getComments());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void createManualDonation_withMinimalRequest_persistsWithDefaults() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("30.00"), "CAD", null, null, null);

        // Act
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert
        List<Donation> donations = donationRepository.findByUserIdOrderByCreatedAtDesc(donorUser.getId());
        assertEquals(1, donations.size());
        
        Donation saved = donations.get(0);
        assertEquals(new BigDecimal("30.00"), saved.getAmount());
        assertNotNull(saved.getCreatedAt());
        assertEquals("Manual Entry", saved.getPaymentProvider());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void createManualDonation_multipleRequests_allPersisted() throws Exception {
        // Arrange
        ManualDonationRequestModel request1 = new ManualDonationRequestModel(
                new BigDecimal("25.00"), "CAD", null, null, "First donation");
        ManualDonationRequestModel request2 = new ManualDonationRequestModel(
                new BigDecimal("40.00"), "CAD", null, null, "Second donation");

        // Act
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Assert
        List<Donation> donations = donationRepository.findByUserIdOrderByCreatedAtDesc(donorUser.getId());
        assertEquals(2, donations.size());
        
        assertEquals(new BigDecimal("40.00"), donations.get(0).getAmount());
        assertEquals(new BigDecimal("25.00"), donations.get(1).getAmount());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void createManualDonation_withNonexistentDonor_returns404NotFound() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("25.00"), "CAD", null, null, null);

        // Act & Assert
        mockMvc.perform(post("/api/employee/donations/manual?donorId=9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void createManualDonation_withUnsupportedCurrency_returns400BadRequest() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("25.00"), "USD", null, null, null);

        // Act & Assert
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void createManualDonation_withAmountTooLow_returns400BadRequest() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("0.50"), "CAD", null, null, null);

        // Act & Assert
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void createManualDonation_commentsAreTrimmedInDatabase() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("20.00"), "CAD", null, null, "   Trimmed comment   ");

        // Act
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert
        List<Donation> donations = donationRepository.findByUserIdOrderByCreatedAtDesc(donorUser.getId());
        assertEquals(1, donations.size());
        assertEquals("Trimmed comment", donations.get(0).getComments());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void createManualDonation_paymentReferenceIsGenerated() throws Exception {
        // Arrange
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("25.00"), "CAD", null, null, null);

        // Act
        mockMvc.perform(post("/api/employee/donations/manual?donorId=" + donorUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert
        List<Donation> donations = donationRepository.findByUserIdOrderByCreatedAtDesc(donorUser.getId());
        assertEquals(1, donations.size());
        assertNotNull(donations.get(0).getPaymentReference());
        assertTrue(donations.get(0).getPaymentReference().startsWith("MANUAL-"));
    }
}
