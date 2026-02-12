package com.mana.openhand_backend.donations.businesslayer;

import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationRepository;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationStatus;
import com.mana.openhand_backend.donations.domainclientlayer.DonationOptionsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DonationServiceImplTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DonationServiceImpl donationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("member@example.com");
        user.setPasswordHash("hashedPassword");
        user.setPreferredLanguage("fr");
        user.setMemberStatus(MemberStatus.ACTIVE);
    }

    @Test
    void getDonationOptions_returnsDefaults() {
        // Arrange

        // Act
        DonationOptionsResponseModel response = donationService.getDonationOptions();

        // Assert
        assertNotNull(response);
        assertEquals("CAD", response.getCurrency());
        assertEquals(new BigDecimal("1.00"), response.getMinimumAmount());
        assertEquals(4, response.getPresetAmounts().size());
        assertTrue(response.getFrequencies().contains("ONE_TIME"));
        assertTrue(response.getFrequencies().contains("MONTHLY"));
    }

    @Test
    void createDonation_withValidRequest_returnsResponseAndNotifies() {
        // Arrange
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "cad", DonationFrequency.ONE_TIME);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation saved = invocation.getArgument(0, Donation.class);
            saved.setId(99L);
            saved.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            return saved;
        });

        // Act
        DonationResponseModel response = donationService.createDonation(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("10.00"), response.getAmount());
        assertEquals("CAD", response.getCurrency());
        assertEquals("ONE_TIME", response.getFrequency());
        assertEquals("RECEIVED", response.getStatus());
        assertNotNull(response.getCreatedAt());
        verify(notificationService).createDonationNotification(1L, "fr");

        ArgumentCaptor<Donation> donationCaptor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(donationCaptor.capture());
        Donation saved = donationCaptor.getValue();
        assertEquals(DonationStatus.RECEIVED, saved.getStatus());
        assertEquals(DonationFrequency.ONE_TIME, saved.getFrequency());
    }

    @Test
    void createDonation_whenUserMissing_throwsNotFound() {
        // Arrange
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "CAD", DonationFrequency.ONE_TIME);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> donationService.createDonation(1L, request));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void createDonation_whenMemberInactive_throwsForbidden() {
        // Arrange
        user.setMemberStatus(MemberStatus.INACTIVE);
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "CAD", DonationFrequency.ONE_TIME);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> donationService.createDonation(1L, request));

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void createDonation_whenAmountBelowMinimum_throwsBadRequest() {
        // Arrange
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("0.50"), "CAD", DonationFrequency.ONE_TIME);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> donationService.createDonation(1L, request));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createDonation_whenCurrencyUnsupported_throwsBadRequest() {
        // Arrange
        DonationRequestModel request = new DonationRequestModel(new BigDecimal("10.00"), "USD", DonationFrequency.ONE_TIME);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> donationService.createDonation(1L, request));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
