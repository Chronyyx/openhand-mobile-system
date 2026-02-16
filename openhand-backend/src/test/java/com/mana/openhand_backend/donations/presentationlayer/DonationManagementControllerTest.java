package com.mana.openhand_backend.donations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.ManualDonationRequestModel;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DonationManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonationManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DonationService donationService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void getDonationsForStaff_returnsList() throws Exception {
        // Arrange
        User employeeUser = new User();
        employeeUser.setId(10L);
        employeeUser.setEmail("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));

        DonationSummaryResponseModel summary = new DonationSummaryResponseModel(
                1L,
                10L,
                200L, // eventId
                "Alex Doe",
                "alex@mana.org",
                new BigDecimal("15.00"),
                "CAD",
                "ONE_TIME",
                "RECEIVED",
                "2025-01-01T10:00:00"
        );
        when(donationService.getDonationsForStaffFilteredFlexible(null, null, null, null, null)).thenReturn(List.of(summary));

        // Act & Assert
        mockMvc.perform(get("/api/employee/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].donorEmail").value("alex@mana.org"));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void createManualDonation_withValidRequest_returns201Created() throws Exception {
        // Arrange
        User employeeUser = new User();
        employeeUser.setId(1L);
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPasswordHash("password");
        employeeUser.setRoles(new java.util.HashSet<>(java.util.List.of("ROLE_EMPLOYEE")));
        employeeUser.setAccountNonLocked(true);
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));
        
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("25.00"), "CAD", 1L, LocalDateTime.of(2025, 1, 15, 14, 30), "Test donation");
        request.setDonorUserId(2L);
        DonationSummaryResponseModel response = new DonationSummaryResponseModel(
                101L, 2L, 201L, "John Donor", "john@example.com", new BigDecimal("25.00"),
                "CAD", "ONE_TIME", "RECEIVED", "2025-01-15T14:30:00");
        when(donationService.createManualDonation(any(Long.class), any(ManualDonationRequestModel.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/employee/donations/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void createManualDonation_withMinimalRequest_returns201Created() throws Exception {
        // Arrange
        User employeeUser = new User();
        employeeUser.setId(1L);
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPasswordHash("password");
        employeeUser.setRoles(new java.util.HashSet<>(java.util.List.of("ROLE_EMPLOYEE")));
        employeeUser.setAccountNonLocked(true);
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));
        
        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("15.00"), "CAD", null, null, null);
        request.setDonorUserId(3L);
        DonationSummaryResponseModel response = new DonationSummaryResponseModel(
                102L, 3L, 202L, "Jane Donor", "jane@example.com", new BigDecimal("15.00"),
                "CAD", "ONE_TIME", "RECEIVED", "2025-01-01T00:00:00");
        when(donationService.createManualDonation(any(Long.class), any(ManualDonationRequestModel.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/employee/donations/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.amount").value(15.00));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void createManualDonation_withLegacyDonorIdParam_mapsToRequestDonorUserId() throws Exception {
        User employeeUser = new User();
        employeeUser.setId(1L);
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPasswordHash("password");
        employeeUser.setRoles(new java.util.HashSet<>(java.util.List.of("ROLE_EMPLOYEE")));
        employeeUser.setAccountNonLocked(true);
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));

        ManualDonationRequestModel request = new ManualDonationRequestModel(
                new BigDecimal("18.00"), "CAD", null, null, null);
        DonationSummaryResponseModel response = new DonationSummaryResponseModel(
                201L, 55L, null, "Legacy Donor", "legacy@example.com", new BigDecimal("18.00"),
                "CAD", "ONE_TIME", "RECEIVED", "2025-01-01T00:00:00");
        when(donationService.createManualDonation(any(Long.class), any(ManualDonationRequestModel.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/employee/donations/manual?donorId=55")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(201))
                .andExpect(jsonPath("$.userId").value(55));
    }
}
