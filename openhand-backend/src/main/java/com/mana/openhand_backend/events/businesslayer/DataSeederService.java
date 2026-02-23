package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataSeederService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;

    public DataSeederService(EventRepository eventRepository, UserRepository userRepository,
            RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public String seedMassiveAnalyticsData() {
        Random random = new Random();
        List<User> users = new ArrayList<>();

        // Generate 1500 users to ensure plenty of people
        for (int i = 0; i < 1500; i++) {
            User u = new User("seeded_user_" + UUID.randomUUID() + "@example.com", "dummyhash", Set.of("ROLE_USER"));
            u.setName("Seeded User " + i);
            u.setAge(18 + random.nextInt(60));
            users.add(u);
        }
        userRepository.saveAll(users);

        // Generate Events spread across the year
        List<EventDefinition> definitions = List.of(
                new EventDefinition("Summer Charity Gala", LocalDateTime.now().minusMonths(6), 800, "Fundraiser"),
                new EventDefinition("Winter Volunteer Drive", LocalDateTime.now().minusMonths(2), 600, "Volunteering"),
                new EventDefinition("Spring Community Picnic", LocalDateTime.now().minusMonths(9), 1000, "Community"),
                new EventDefinition("Annual Tech Workshop", LocalDateTime.now().plusMonths(3), 400, "Workshop"),
                new EventDefinition("End of Year Banquet", LocalDateTime.now().plusMonths(7), 700, "Fundraiser"));

        int totalRegistrationsCount = 0;

        for (EventDefinition def : definitions) {
            Event event = new Event(
                    def.title,
                    "A massive seeded event for analytics. This helps populate the YouTube Studio-style dashboard.",
                    def.startDate,
                    def.startDate.plusHours(4),
                    "Grand Hall",
                    "123 Main St",
                    EventStatus.OPEN,
                    def.capacity,
                    0,
                    def.category);
            event = eventRepository.save(event);

            // Target 90% to 150% of capacity to see waitlists and un-filled events (Bias
            // towards full)
            int targetRegistrations = (int) (def.capacity * (0.9 + random.nextDouble() * 0.6));

            List<Registration> registrationsToSave = new ArrayList<>();
            int currentConfirmed = 0;
            int waitlistCounter = 1;

            // Randomize how full the confirmed slots get (70-100% of capacity)
            // This prevents every event from rounding up to exactly full
            int confirmedCap = (int) (def.capacity * (0.70 + random.nextDouble() * 0.30));

            Collections.shuffle(users);

            for (int i = 0; i < Math.min(targetRegistrations, users.size()); i++) {
                User u = users.get(i);
                Registration r = new Registration(u, event);

                // Spread registrations over a realistic timeframe
                // For future events: spread from today backwards (so data shows up in past)
                // For past events: spread backwards from the event start date
                LocalDateTime referenceDate = event.getStartDateTime().isBefore(LocalDateTime.now())
                        ? event.getStartDateTime()
                        : LocalDateTime.now();

                // Use a wide spread: 1 to 180 days before reference, Gaussian centered ~60 days
                // out
                double gaussian = random.nextGaussian(); // mean 0, stddev 1
                int daysBefore = (int) (60 + gaussian * 40);
                if (daysBefore < 1)
                    daysBefore = 1;
                if (daysBefore > 180)
                    daysBefore = 180;

                LocalDateTime requestedAt = referenceDate.minusDays(daysBefore)
                        .minusHours(random.nextInt(24));
                r.setRequestedAt(requestedAt);

                if (currentConfirmed < confirmedCap) {
                    // 90% chance to be confirmed immediately, 10% cancelled
                    if (random.nextDouble() < 0.9) {
                        r.setStatus(RegistrationStatus.CONFIRMED);
                        int confirmDelay = Math.max(0, random.nextInt(48));
                        r.setConfirmedAt(requestedAt.plusHours(confirmDelay));
                        currentConfirmed++;
                    } else {
                        r.setStatus(RegistrationStatus.CANCELLED);
                        int cancelDelay = Math.max(1, random.nextInt(10));
                        r.setCancelledAt(requestedAt.plusDays(cancelDelay));
                    }
                } else {
                    // Waitlist probability 90% to build a nice waitlist wedge on pie chart
                    if (random.nextDouble() < 0.9) {
                        r.setStatus(RegistrationStatus.WAITLISTED);
                        r.setWaitlistedPosition(waitlistCounter++);
                    } else {
                        r.setStatus(RegistrationStatus.CANCELLED);
                        int cancelDelay = Math.max(1, random.nextInt(5));
                        r.setCancelledAt(requestedAt.plusDays(cancelDelay));
                    }
                }
                registrationsToSave.add(r);
            }
            registrationRepository.saveAll(registrationsToSave);
            totalRegistrationsCount += registrationsToSave.size();

            // Update event counts
            event.setCurrentRegistrations(currentConfirmed);
            event.setTotalRegistrations(registrationsToSave.size());
            if (waitlistCounter > 1) {
                event.setStatus(EventStatus.FULL);
            }
            eventRepository.save(event);
        }

        return "Successfully seeded 5 events, 1500 users, and " + totalRegistrationsCount
                + " total registration data points!";
    }

    private static class EventDefinition {
        String title;
        LocalDateTime startDate;
        int capacity;
        String category;

        EventDefinition(String title, LocalDateTime startDate, int capacity, String category) {
            this.title = title;
            this.startDate = startDate;
            this.capacity = capacity;
            this.category = category;
        }
    }
}
