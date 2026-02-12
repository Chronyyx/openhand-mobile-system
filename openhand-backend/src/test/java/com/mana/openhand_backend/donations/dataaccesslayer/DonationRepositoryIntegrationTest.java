package com.mana.openhand_backend.donations.dataaccesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class DonationRepositoryIntegrationTest {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("member@example.com");
        user.setPasswordHash("hashedPassword");
        user = userRepository.save(user);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsDescendingResults() {
        // Arrange
        Donation older = new Donation(user, new BigDecimal("5.00"), "CAD", DonationFrequency.ONE_TIME,
                DonationStatus.RECEIVED);
        older.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));
        donationRepository.save(older);

        Donation newer = new Donation(user, new BigDecimal("15.00"), "CAD", DonationFrequency.MONTHLY,
                DonationStatus.RECEIVED);
        newer.setCreatedAt(LocalDateTime.of(2025, 1, 2, 9, 0));
        donationRepository.save(newer);
        entityManager.flush();

        // Act
        List<Donation> results = donationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Assert
        assertEquals(2, results.size());
        assertEquals(new BigDecimal("15.00"), results.get(0).getAmount());
        assertEquals(new BigDecimal("5.00"), results.get(1).getAmount());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_whenNone_returnsEmptyList() {
        // Arrange

        // Act
        List<Donation> results = donationRepository.findByUserIdOrderByCreatedAtDesc(999L);

        // Assert
        assertTrue(results.isEmpty());
    }
}
