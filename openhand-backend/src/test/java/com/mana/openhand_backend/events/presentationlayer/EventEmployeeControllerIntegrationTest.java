package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventEmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationRepository.flush();
        registrationRepository.deleteAll();
        registrationRepository.flush();
        eventRepository.deleteAll();
        eventRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void deleteArchivedEvent_asEmployee_removesEventAndDependencies() throws Exception {
        User user = createAndSaveUser("participant@example.com");
        Event event = createAndSaveEvent(EventStatus.COMPLETED);

        Registration registration = new Registration(user, event);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(registration);

        Notification notification = new Notification(
                user,
                event,
                NotificationType.CANCELLATION,
                "en",
                "Archived event removed",
                event.getTitle()
        );
        notificationRepository.save(notification);

        mockMvc.perform(delete("/api/employee/events/{id}", event.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(eventRepository.findById(event.getId()).isEmpty());
        assertEquals(0, registrationRepository.findByEventId(event.getId()).size());
        assertEquals(0, notificationRepository.count());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void deleteArchivedEvent_whenNotArchived_returns400() throws Exception {
        Event event = createAndSaveEvent(EventStatus.OPEN);

        mockMvc.perform(delete("/api/employee/events/{id}", event.getId())
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        assertTrue(eventRepository.findById(event.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void deleteArchivedEvent_asMember_returnsForbidden() throws Exception {
        Event event = createAndSaveEvent(EventStatus.COMPLETED);

        mockMvc.perform(delete("/api/employee/events/{id}", event.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(eventRepository.findById(event.getId()).isPresent());
    }

    private User createAndSaveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setRoles(new HashSet<>());
        return userRepository.save(user);
    }

    private Event createAndSaveEvent(EventStatus status) {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        Event event = new Event(
                "Archived Event",
                "Archived Event Description",
                start,
                end,
                "Test Location",
                "Test Address",
                status,
                10,
                0,
                "Test"
        );
        return eventRepository.save(event);
    }
}
