package com.mana.openhand_backend.donations.businesslayer;

import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationRepository;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DonationServiceImplFilterTest {
    @Mock
    private DonationRepository donationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private com.mana.openhand_backend.events.dataaccesslayer.EventRepository eventRepository;
    @InjectMocks
    private DonationServiceImpl donationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        donationService = new DonationServiceImpl(
                donationRepository,
                userRepository,
                notificationService,
                notificationRepository,
                eventRepository
        );
    }

    @Test
    void filterByEventId_onlyReturnsMatchingDonations() {
        // Arrange
        var event = new com.mana.openhand_backend.events.dataaccesslayer.Event(
            "Spring Gala", "desc", LocalDateTime.now(), LocalDateTime.now(), "loc", "addr",
            com.mana.openhand_backend.events.dataaccesslayer.EventStatus.OPEN, 100, 0, "cat");
        event.setId(100L);
        var donation1 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation1.setId(1L);
        donation1.setEvent(event);
        donation1.setCreatedAt(LocalDateTime.of(2024, 5, 10, 12, 0));
        var donation2 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation2.setId(2L);
        donation2.setEvent(null);
        donation2.setCreatedAt(LocalDateTime.of(2024, 5, 10, 12, 0));
        when(donationRepository.findAll()).thenReturn(List.of(donation1, donation2));
        // Act
        var result = donationService.getDonationsForStaffFilteredFlexible(100L, null, null, null, null);
        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void filterByCampaignName_caseInsensitive() {
        // Arrange
        var event = new com.mana.openhand_backend.events.dataaccesslayer.Event(
            "Summer Fundraiser", "desc", LocalDateTime.now(), LocalDateTime.now(), "loc", "addr",
            com.mana.openhand_backend.events.dataaccesslayer.EventStatus.OPEN, 100, 0, "cat");
        event.setId(101L);
        var donation = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation.setId(3L);
        donation.setEvent(event);
        donation.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 0));
        when(donationRepository.findAll()).thenReturn(List.of(donation));
        // Act
        var result = donationService.getDonationsForStaffFilteredFlexible(null, "summer fundraiser", null, null, null);
        // Assert
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void filterByYearMonthDay_matchesCorrectly() {
        // Arrange
        var event = new com.mana.openhand_backend.events.dataaccesslayer.Event(
            "Autumn Drive", "desc", LocalDateTime.now(), LocalDateTime.now(), "loc", "addr",
            com.mana.openhand_backend.events.dataaccesslayer.EventStatus.OPEN, 100, 0, "cat");
        event.setId(102L);
        var donation1 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation1.setId(4L);
        donation1.setEvent(event);
        donation1.setCreatedAt(LocalDateTime.of(2023, 9, 5, 8, 0));
        var donation2 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation2.setId(5L);
        donation2.setEvent(event);
        donation2.setCreatedAt(LocalDateTime.of(2023, 9, 6, 8, 0));
        when(donationRepository.findAll()).thenReturn(List.of(donation1, donation2));
        // Act
        var result = donationService.getDonationsForStaffFilteredFlexible(null, null, 2023, 9, 5);
        // Assert
        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getId());
    }

    @Test
    void filterByYearAndMonth_matchesAllDaysInMonth() {
        // Arrange
        var event = new com.mana.openhand_backend.events.dataaccesslayer.Event(
            "Winter Campaign", "desc", LocalDateTime.now(), LocalDateTime.now(), "loc", "addr",
            com.mana.openhand_backend.events.dataaccesslayer.EventStatus.OPEN, 100, 0, "cat");
        event.setId(103L);
        var donation1 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation1.setId(6L);
        donation1.setEvent(event);
        donation1.setCreatedAt(LocalDateTime.of(2022, 12, 1, 8, 0));
        var donation2 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation2.setId(7L);
        donation2.setEvent(event);
        donation2.setCreatedAt(LocalDateTime.of(2022, 12, 31, 8, 0));
        when(donationRepository.findAll()).thenReturn(List.of(donation1, donation2));
        // Act
        var result = donationService.getDonationsForStaffFilteredFlexible(null, null, 2022, 12, null);
        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void filterByYear_matchesAllMonthsAndDays() {
        // Arrange
        var event = new com.mana.openhand_backend.events.dataaccesslayer.Event(
            "Year End", "desc", LocalDateTime.now(), LocalDateTime.now(), "loc", "addr",
            com.mana.openhand_backend.events.dataaccesslayer.EventStatus.OPEN, 100, 0, "cat");
        event.setId(104L);
        var donation1 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation1.setId(8L);
        donation1.setEvent(event);
        donation1.setCreatedAt(LocalDateTime.of(2021, 1, 1, 8, 0));
        var donation2 = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation2.setId(9L);
        donation2.setEvent(event);
        donation2.setCreatedAt(LocalDateTime.of(2021, 12, 31, 8, 0));
        when(donationRepository.findAll()).thenReturn(List.of(donation1, donation2));
        // Act
        var result = donationService.getDonationsForStaffFilteredFlexible(null, null, 2021, null, null);
        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void getDonationReportByDateRange_returnsOnlyDonationsInsideRange() {
        var donationInRange = new Donation(
                null, new BigDecimal("15.00"), "CAD",
                DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donationInRange.setId(11L);
        donationInRange.setCreatedAt(LocalDateTime.of(2025, 1, 10, 9, 0));

        when(donationRepository.findByCreatedAtBetweenWithUserOrderByCreatedAtDesc(
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 31, 23, 59, 59, 999999999)))
                .thenReturn(List.of(donationInRange));

        var result = donationService.getDonationReportByDateRange(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        assertEquals(new BigDecimal("15.00"), result.get(0).getAmount());
    }

    @Test
    void filterWithNoParams_returnsAll() {
        // Arrange
        var event = new com.mana.openhand_backend.events.dataaccesslayer.Event(
            "General Event", "desc", LocalDateTime.now(), LocalDateTime.now(), "loc", "addr",
            com.mana.openhand_backend.events.dataaccesslayer.EventStatus.OPEN, 100, 0, "cat");
        event.setId(105L);
        var donation = new Donation(
            null, new BigDecimal("10.00"), "CAD",
            DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        donation.setId(10L);
        donation.setEvent(event);
        donation.setCreatedAt(LocalDateTime.of(2020, 7, 20, 8, 0));
        when(donationRepository.findAll()).thenReturn(List.of(donation));
        // Act
        var result = donationService.getDonationsForStaffFilteredFlexible(null, null, null, null, null);
        // Assert
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }
}
