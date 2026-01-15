package com.mana.openhand_backend.notifications.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceItemRequestModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceUpdateRequestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationPreferenceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    private static final String TEST_USER_EMAIL = "notification_prefs_test@example.com";

    @BeforeEach
    @Transactional
    void setUp() {
        preferenceRepository.deleteAll();
        userRepository.findByEmail(TEST_USER_EMAIL).orElseGet(() -> {
            User newUser = new User(TEST_USER_EMAIL, "password123", Set.of("ROLE_MEMBER"));
            newUser.setPreferredLanguage("en");
            return userRepository.save(newUser);
        });
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void getPreferences_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId", notNullValue()))
                .andExpect(jsonPath("$.preferences", hasSize(3)))
                .andExpect(jsonPath("$.preferences[*].category", containsInAnyOrder(
                        "CONFIRMATION", "REMINDER", "CANCELLATION"
                )));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void updatePreferences_updatesNonCriticalCategory() throws Exception {
        NotificationPreferenceUpdateRequestModel request = new NotificationPreferenceUpdateRequestModel(
                List.of(new NotificationPreferenceItemRequestModel("REMINDER", false))
        );

        mockMvc.perform(put("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences[?(@.category=='REMINDER')].enabled", contains(false)));

        NotificationPreference preference = preferenceRepository.findByUserId(
                userRepository.findByEmail(TEST_USER_EMAIL).orElseThrow().getId()
        ).orElseThrow();

        if (preference.isReminderEnabled()) {
            throw new AssertionError("Expected reminder preference to be disabled.");
        }
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void updatePreferences_disableCriticalCategory_returnsBadRequest() throws Exception {
        NotificationPreferenceUpdateRequestModel request = new NotificationPreferenceUpdateRequestModel(
                List.of(new NotificationPreferenceItemRequestModel("CANCELLATION", false))
        );

        mockMvc.perform(put("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPreferences_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePreferences_unauthenticated_returnsUnauthorized() throws Exception {
        NotificationPreferenceUpdateRequestModel request = new NotificationPreferenceUpdateRequestModel(
                List.of(new NotificationPreferenceItemRequestModel("REMINDER", false))
        );

        mockMvc.perform(put("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
