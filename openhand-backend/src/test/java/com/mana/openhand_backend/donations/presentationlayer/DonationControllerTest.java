package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import com.mana.openhand_backend.donations.domainclientlayer.DonationOptionsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationControllerTest {

    @Mock
    private DonationService donationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private DonationController donationController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("member@example.com");
        user.setPasswordHash("hashedPassword");
    }

    private void setupAuthentication(String email) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Test
    void getDonationOptions_returnsServiceResponse() {
        // Arrange
        DonationOptionsResponseModel response = new DonationOptionsResponseModel(
                "CAD",
                new BigDecimal("1.00"),
                List.of(10, 25, 50, 100),
                List.of("ONE_TIME", "MONTHLY"));
        when(donationService.getDonationOptions()).thenReturn(response);

        // Act
        DonationOptionsResponseModel result = donationController.getDonationOptions();

        // Assert
        assertNotNull(result);
        assertEquals("CAD", result.getCurrency());
        verify(donationService).getDonationOptions();
    }

    @Test
    void submitDonation_withValidRequest_callsService() {
        // Arrange
        setupAuthentication("member@example.com");
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "CAD", DonationFrequency.ONE_TIME);
        DonationResponseModel response = new DonationResponseModel(1L, new BigDecimal("10.00"), "CAD",
                "ONE_TIME", "RECEIVED", "2025-01-01T10:00:00", "Donation received.");
        when(donationService.createDonation(1L, request)).thenReturn(response);

        // Act
        DonationResponseModel result = donationController.submitDonation(request, authentication);

        // Assert
        assertNotNull(result);
        assertEquals("RECEIVED", result.getStatus());
        verify(donationService).createDonation(1L, request);
    }

    @Test
    void submitDonation_whenUserMissing_throwsException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "CAD", DonationFrequency.ONE_TIME);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> donationController.submitDonation(request, authentication));

        // Assert
        assertEquals("User not found with email: missing@example.com", exception.getMessage());
    }
}
