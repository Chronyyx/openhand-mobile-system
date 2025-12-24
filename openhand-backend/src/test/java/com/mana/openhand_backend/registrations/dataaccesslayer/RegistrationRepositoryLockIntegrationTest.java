package com.mana.openhand_backend.registrations.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RegistrationRepositoryLockIntegrationTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Event event;
    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail("locktest@example.com");
        user.setPasswordHash("pw");
        userRepository.save(user);

        event = new Event(
                "Lock Event",
                "",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "Loc",
                "",
                EventStatus.OPEN,
                1,
                0,
                "General");
        event = eventRepository.save(event);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findEventByIdForUpdate_concurrentTransactions_updateSequentially() throws Exception {
        // Arrange
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        DefaultTransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionTemplate template = new TransactionTemplate(transactionManager, def);

        // Ensure the event exists in a committed transaction visible to both threads
        event = template.execute(status -> {
            Event fresh = new Event(
                    event.getTitle(),
                    event.getDescription(),
                    event.getStartDateTime(),
                    event.getEndDateTime(),
                    event.getLocationName(),
                    event.getAddress(),
                    EventStatus.OPEN,
                    1,
                    0,
                    event.getCategory());
            return eventRepository.save(fresh);
        });

        Runnable t1 = () -> {
            try {
                start.await();
                template.execute(status -> {
                    Event locked = registrationRepository.findEventByIdForUpdate(event.getId()).orElseThrow();
                    Integer current = locked.getCurrentRegistrations() == null ? 0 : locked.getCurrentRegistrations();
                    locked.setCurrentRegistrations(current + 1);
                    eventRepository.save(locked);
                    return null;
                });
            } catch (Exception e) {
                fail("T1 failed: " + e.getMessage());
            } finally {
                done.countDown();
            }
        };

        Runnable t2 = () -> {
            try {
                start.await();
                template.execute(status -> {
                    Event locked = registrationRepository.findEventByIdForUpdate(event.getId()).orElseThrow();
                    // No-op, just ensure we can read after T1 commits
                    return null;
                });
            } catch (Exception e) {
                fail("T2 failed: " + e.getMessage());
            } finally {
                done.countDown();
            }
        };

        Thread th1 = new Thread(t1);
        Thread th2 = new Thread(t2);

        // Act
        th1.start();
        th2.start();
        start.countDown();
        done.await();

        // Assert
        TransactionTemplate readTemplate = new TransactionTemplate(transactionManager, def);
        Event reloaded = readTemplate.execute(status -> eventRepository.findById(event.getId()).orElseThrow());
        assertEquals(1, reloaded.getCurrentRegistrations());
    }
}
