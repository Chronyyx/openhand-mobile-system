package com.mana.openhand_backend.donations.businesslayer;

import com.mana.openhand_backend.donations.dataaccesslayer.Donation;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationFrequency;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationRepository;
import com.mana.openhand_backend.donations.dataaccesslayer.DonationStatus;
import com.mana.openhand_backend.donations.domainclientlayer.DonationOptionsResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationRequestModel;
import com.mana.openhand_backend.donations.domainclientlayer.DonationResponseModel;
import com.mana.openhand_backend.donations.utils.DonationResponseMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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

    public DonationServiceImpl(DonationRepository donationRepository, UserRepository userRepository,
            NotificationService notificationService) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
}
