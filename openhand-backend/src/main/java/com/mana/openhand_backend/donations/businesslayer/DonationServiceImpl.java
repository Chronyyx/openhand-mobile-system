package com.mana.openhand_backend.donations.businesslayer;

import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationRepository;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationStatus;
import com.mana.openhand_backend.donations.domainclientlayer.DonationDetailResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricBreakdownResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMetricsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationMonthlyTrendResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationOptionsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationTopDonorResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.ManualDonationRequestModel;
import com.mana.openhand_backend.donations.utils.DonationManagementMapper;
import com.mana.openhand_backend.donations.utils.DonationResponseMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonationServiceImpl implements DonationService {

    private static final String DEFAULT_CURRENCY = "CAD";
    private static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("1.00");
    private static final List<Integer> PRESET_AMOUNTS = List.of(10, 25, 50, 100);

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public DonationServiceImpl(DonationRepository donationRepository, UserRepository userRepository,
            NotificationService notificationService, NotificationRepository notificationRepository) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public DonationOptionsResponseModel getDonationOptions() {
        List<String> frequencies = List.of(DonationFrequency.ONE_TIME.name(), DonationFrequency.MONTHLY.name());
        return new DonationOptionsResponseModel(DEFAULT_CURRENCY, MINIMUM_AMOUNT, PRESET_AMOUNTS, frequencies);
    }

    @Override
    @Transactional
    public DonationResponseModel createDonation(Long userId, DonationRequestModel request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (user.getMemberStatus() != null && Objects.equals(user.getMemberStatus().name(), "INACTIVE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inactive members cannot donate.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(MINIMUM_AMOUNT) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Donation amount must be at least " + MINIMUM_AMOUNT + ".");
        }

        String currency = request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "";
        if (!DEFAULT_CURRENCY.equals(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported currency: " + currency);
        }

        Donation donation = new Donation(user, request.getAmount(), currency, request.getFrequency(),
                DonationStatus.RECEIVED);
        Donation saved = donationRepository.save(donation);

        String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
        notificationService.createDonationNotification(userId, language);

        return DonationResponseMapper.toResponseModel(saved,
            "Donation received. Thank you for your support.");
    }

    @Override
    public List<DonationSummaryResponseModel> getDonationsForStaff() {
        return donationRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(DonationManagementMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public DonationDetailResponseModel getDonationDetail(Long donationId) {
        Donation donation = donationRepository.findByIdWithUser(donationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found."));
        return DonationManagementMapper.toDetail(donation);
    }

    @Override
    @Transactional
    public DonationSummaryResponseModel createManualDonation(Long employeeId, Long donorUserId, 
                                                             ManualDonationRequestModel request) {
        User donorUser = userRepository.findById(donorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor user not found."));

        if (request.getAmount() == null || request.getAmount().compareTo(MINIMUM_AMOUNT) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Donation amount must be at least " + MINIMUM_AMOUNT + ".");
        }

        String currency = request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : DEFAULT_CURRENCY;
        if (!DEFAULT_CURRENCY.equals(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported currency: " + currency);
        }

        Donation donation = new Donation(donorUser, request.getAmount(), currency, 
                DonationFrequency.ONE_TIME, DonationStatus.RECEIVED);
        
        // Set donation date if provided, otherwise use current time
        if (request.getDonationDate() != null) {
            donation.setCreatedAt(request.getDonationDate());
        } else {
            donation.setCreatedAt(LocalDateTime.now());
        }

        // Set optional comments
        if (request.getComments() != null && !request.getComments().trim().isEmpty()) {
            donation.setComments(request.getComments().trim());
        }

        // Mark as manually entered by employee
        donation.setPaymentProvider("Manual Entry");
        donation.setPaymentReference("MANUAL-" + employeeId + "-" + System.currentTimeMillis());

        Donation saved = donationRepository.save(donation);
        return DonationManagementMapper.toSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationMetricsResponseModel getDonationMetrics() {
        List<Donation> donations = donationRepository.findAllWithUserOrderByCreatedAtDesc();
        long totalDonations = donations.size();
        BigDecimal totalAmount = sumAmounts(donations);
        BigDecimal averageAmount = totalDonations > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalDonations), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<Long, List<Donation>> donationsByDonor = donations.stream()
                .filter(donation -> donation.getUser() != null && donation.getUser().getId() != null)
                .collect(Collectors.groupingBy(donation -> donation.getUser().getId()));

        long uniqueDonors = donationsByDonor.size();
        long repeatDonors = donationsByDonor.values().stream().filter(donorDonations -> donorDonations.size() > 1).count();
        long firstTimeDonors = donationsByDonor.values().stream().filter(donorDonations -> donorDonations.size() == 1).count();

        List<DonationMetricBreakdownResponseModel> frequencyBreakdown = buildFrequencyBreakdown(donations);
        List<DonationMetricBreakdownResponseModel> statusBreakdown = buildStatusBreakdown(donations);
        List<DonationMonthlyTrendResponseModel> monthlyTrend = buildMonthlyTrend(donations);
        List<DonationTopDonorResponseModel> topDonorsByAmount = buildTopDonorsByAmount(donations, 5);
        List<DonationTopDonorResponseModel> topDonorsByCount = buildTopDonorsByCount(donations, 5);

        long manualDonationsCount = donations.stream().filter(this::isManualDonation).count();
        BigDecimal manualDonationsAmount = donations.stream()
                .filter(this::isManualDonation)
                .map(Donation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long externalDonationsCount = totalDonations - manualDonationsCount;
        BigDecimal externalDonationsAmount = totalAmount.subtract(manualDonationsAmount);

        long commentsCount = donations.stream()
                .filter(donation -> donation.getComments() != null && !donation.getComments().trim().isEmpty())
                .count();
        double commentsUsageRate = totalDonations > 0
                ? BigDecimal.valueOf((commentsCount * 100.0d) / totalDonations)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0d;

        long donationNotificationsCreated = notificationRepository.countByNotificationType(NotificationType.DONATION_CONFIRMATION);
        long donationNotificationsRead = notificationRepository
                .countByNotificationTypeAndIsReadTrue(NotificationType.DONATION_CONFIRMATION);
        long donationNotificationsUnread = notificationRepository
                .countByNotificationTypeAndIsReadFalse(NotificationType.DONATION_CONFIRMATION);

        return new DonationMetricsResponseModel(
                DEFAULT_CURRENCY,
                totalDonations,
                totalAmount,
                averageAmount,
                uniqueDonors,
                repeatDonors,
                firstTimeDonors,
                frequencyBreakdown,
                statusBreakdown,
                monthlyTrend,
                topDonorsByAmount,
                topDonorsByCount,
                manualDonationsCount,
                manualDonationsAmount,
                externalDonationsCount,
                externalDonationsAmount,
                commentsCount,
                commentsUsageRate,
                donationNotificationsCreated,
                donationNotificationsRead,
                donationNotificationsUnread
        );
    }

    private BigDecimal sumAmounts(List<Donation> donations) {
        return donations.stream()
                .map(Donation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isManualDonation(Donation donation) {
        return donation.getPaymentProvider() != null
                && "Manual Entry".equalsIgnoreCase(donation.getPaymentProvider().trim());
    }

    private List<DonationMetricBreakdownResponseModel> buildFrequencyBreakdown(List<Donation> donations) {
        Map<String, List<Donation>> grouped = donations.stream()
                .collect(Collectors.groupingBy(
                        donation -> donation.getFrequency() != null ? donation.getFrequency().name() : "UNKNOWN"));
        List<String> preferredOrder = List.of(
                DonationFrequency.ONE_TIME.name(),
                DonationFrequency.MONTHLY.name(),
                "UNKNOWN"
        );
        return buildBreakdown(grouped, preferredOrder);
    }

    private List<DonationMetricBreakdownResponseModel> buildStatusBreakdown(List<Donation> donations) {
        Map<String, List<Donation>> grouped = donations.stream()
                .collect(Collectors.groupingBy(
                        donation -> donation.getStatus() != null ? donation.getStatus().name() : "UNKNOWN"));
        List<String> preferredOrder = List.of(
                DonationStatus.RECEIVED.name(),
                DonationStatus.FAILED.name(),
                "UNKNOWN"
        );
        return buildBreakdown(grouped, preferredOrder);
    }

    private List<DonationMetricBreakdownResponseModel> buildBreakdown(
            Map<String, List<Donation>> grouped, List<String> preferredOrder) {
        LinkedHashSet<String> orderedKeys = new LinkedHashSet<>();
        for (String preferredKey : preferredOrder) {
            if (grouped.containsKey(preferredKey)) {
                orderedKeys.add(preferredKey);
            }
        }

        grouped.keySet().stream()
                .filter(key -> !orderedKeys.contains(key))
                .sorted()
                .forEach(orderedKeys::add);

        List<DonationMetricBreakdownResponseModel> result = new ArrayList<>();
        for (String key : orderedKeys) {
            List<Donation> bucket = grouped.getOrDefault(key, List.of());
            result.add(new DonationMetricBreakdownResponseModel(
                    key,
                    bucket.size(),
                    sumAmounts(bucket)
            ));
        }
        return result;
    }

    private List<DonationMonthlyTrendResponseModel> buildMonthlyTrend(List<Donation> donations) {
        Map<YearMonth, List<Donation>> grouped = donations.stream()
                .filter(donation -> donation.getCreatedAt() != null)
                .collect(Collectors.groupingBy(donation -> YearMonth.from(donation.getCreatedAt())));

        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(5);
        List<DonationMonthlyTrendResponseModel> result = new ArrayList<>();

        for (YearMonth month = start; !month.isAfter(current); month = month.plusMonths(1)) {
            List<Donation> monthDonations = grouped.getOrDefault(month, List.of());
            result.add(new DonationMonthlyTrendResponseModel(
                    month.toString(),
                    monthDonations.size(),
                    sumAmounts(monthDonations)
            ));
        }

        return result;
    }

    private List<DonationTopDonorResponseModel> buildTopDonorsByAmount(List<Donation> donations, int limit) {
        return aggregateByDonor(donations).values().stream()
                .sorted(
                        Comparator.comparing(DonorAggregate::getTotalAmount, BigDecimal::compareTo).reversed()
                                .thenComparing(DonorAggregate::getDonationCount, Comparator.reverseOrder())
                )
                .limit(limit)
                .map(aggregate -> new DonationTopDonorResponseModel(
                        aggregate.getUserId(),
                        aggregate.getDonorName(),
                        aggregate.getDonorEmail(),
                        aggregate.getDonationCount(),
                        aggregate.getTotalAmount()
                ))
                .collect(Collectors.toList());
    }

    private List<DonationTopDonorResponseModel> buildTopDonorsByCount(List<Donation> donations, int limit) {
        return aggregateByDonor(donations).values().stream()
                .sorted(
                        Comparator.comparing(DonorAggregate::getDonationCount, Comparator.reverseOrder())
                                .thenComparing(DonorAggregate::getTotalAmount, Comparator.reverseOrder())
                )
                .limit(limit)
                .map(aggregate -> new DonationTopDonorResponseModel(
                        aggregate.getUserId(),
                        aggregate.getDonorName(),
                        aggregate.getDonorEmail(),
                        aggregate.getDonationCount(),
                        aggregate.getTotalAmount()
                ))
                .collect(Collectors.toList());
    }

    private Map<Long, DonorAggregate> aggregateByDonor(List<Donation> donations) {
        Map<Long, DonorAggregate> aggregates = new HashMap<>();
        for (Donation donation : donations) {
            User donor = donation.getUser();
            if (donor == null || donor.getId() == null) {
                continue;
            }

            DonorAggregate aggregate = aggregates.computeIfAbsent(
                    donor.getId(),
                    key -> new DonorAggregate(donor.getId(), donor.getName(), donor.getEmail())
            );

            aggregate.incrementDonationCount();
            if (donation.getAmount() != null) {
                aggregate.addAmount(donation.getAmount());
            }
        }
        return aggregates;
    }

    private static final class DonorAggregate {
        private final Long userId;
        private final String donorName;
        private final String donorEmail;
        private long donationCount;
        private BigDecimal totalAmount;

        private DonorAggregate(Long userId, String donorName, String donorEmail) {
            this.userId = userId;
            this.donorName = donorName;
            this.donorEmail = donorEmail;
            this.donationCount = 0L;
            this.totalAmount = BigDecimal.ZERO;
        }

        private Long getUserId() {
            return userId;
        }

        private String getDonorName() {
            return donorName;
        }

        private String getDonorEmail() {
            return donorEmail;
        }

        private long getDonationCount() {
            return donationCount;
        }

        private BigDecimal getTotalAmount() {
            return totalAmount;
        }

        private void incrementDonationCount() {
            this.donationCount += 1;
        }

        private void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
        }
    }
}
