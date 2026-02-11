package com.mana.openhand_backend.identity.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateSecuritySettingsRequest;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class UserSecuritySettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String PRIMARY_EMAIL = "security.settings.primary@example.com";
    private static final String SECONDARY_EMAIL = "security.settings.secondary@example.com";

    @BeforeEach
    @Transactional
    void setUp() {
        userRepository.deleteAll();

        User primary = new User(PRIMARY_EMAIL, "password", Set.of("ROLE_MEMBER"));
        primary.setBiometricsEnabled(false);
        userRepository.save(primary);

        User secondary = new User(SECONDARY_EMAIL, "password", Set.of("ROLE_MEMBER"));
        secondary.setBiometricsEnabled(false);
        userRepository.save(secondary);
    }

    @Test
    @WithMockUser(username = PRIMARY_EMAIL, roles = { "MEMBER" })
    void getSecuritySettings_authenticatedUser_canReadSetting() throws Exception {
        mockMvc.perform(get("/api/users/me/security-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricsEnabled").value(false));
    }

    @Test
    @WithMockUser(username = PRIMARY_EMAIL, roles = { "MEMBER" })
    void putSecuritySettings_authenticatedUser_canEnableAndDisable() throws Exception {
        UpdateSecuritySettingsRequest enableRequest = new UpdateSecuritySettingsRequest();
        enableRequest.setBiometricsEnabled(true);

        mockMvc.perform(put("/api/users/me/security-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enableRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricsEnabled").value(true));

        UpdateSecuritySettingsRequest disableRequest = new UpdateSecuritySettingsRequest();
        disableRequest.setBiometricsEnabled(false);

        mockMvc.perform(put("/api/users/me/security-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricsEnabled").value(false));
    }

    @Test
    void getSecuritySettings_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me/security-settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putSecuritySettings_unauthenticated_returnsUnauthorized() throws Exception {
        UpdateSecuritySettingsRequest request = new UpdateSecuritySettingsRequest();
        request.setBiometricsEnabled(true);

        mockMvc.perform(put("/api/users/me/security-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = PRIMARY_EMAIL, roles = { "MEMBER" })
    void putSecuritySettings_meEndpointCannotUpdateAnotherUsersSetting() throws Exception {
        UpdateSecuritySettingsRequest request = new UpdateSecuritySettingsRequest();
        request.setBiometricsEnabled(true);

        mockMvc.perform(put("/api/users/me/security-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricsEnabled").value(true));

        User primary = userRepository.findByEmail(PRIMARY_EMAIL).orElseThrow();
        User secondary = userRepository.findByEmail(SECONDARY_EMAIL).orElseThrow();

        assertThat(primary.isBiometricsEnabled()).isTrue();
        assertThat(secondary.isBiometricsEnabled()).isFalse();
    }
}
