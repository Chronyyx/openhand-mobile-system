package com.mana.openhand_backend.registrations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationRequestModel;
import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.EventCapacityException;
import com.mana.openhand_backend.registrations.utils.EventCompletedException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RegistrationExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setupUser() {
        User user = new User();
        user.setEmail("member@example.com");
        user.setId(1L);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void registerForEvent_whenCapacityExceeded_returnsConflict() throws Exception {
        when(registrationService.registerForEvent(anyLong(), anyLong()))
                .thenThrow(new EventCapacityException(99L));

        RegistrationRequestModel request = new RegistrationRequestModel(99L);

        mockMvc.perform(post("/api/registrations")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("MEMBER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Event Capacity Exceeded"))
                .andExpect(jsonPath("$.eventId").value(99));
    }

    @Test
    void registerForEvent_whenEventCompleted_returnsConflict() throws Exception {
        when(registrationService.registerForEvent(anyLong(), anyLong()))
                .thenThrow(new EventCompletedException(42L));

        RegistrationRequestModel request = new RegistrationRequestModel(42L);

        mockMvc.perform(post("/api/registrations")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("MEMBER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Event Completed"))
                .andExpect(jsonPath("$.eventId").value(42));
    }

    @Test
    void registerForEvent_whenAlreadyRegistered_returnsConflict() throws Exception {
        when(registrationService.registerForEvent(eq(1L), anyLong()))
                .thenThrow(new AlreadyRegisteredException(1L, 10L));

        RegistrationRequestModel request = new RegistrationRequestModel(10L);

        mockMvc.perform(post("/api/registrations")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("MEMBER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Already Registered"));
    }

    @Test
    void cancelRegistration_whenNotFound_returnsNotFound() throws Exception {
        when(registrationService.cancelRegistration(anyLong(), anyLong()))
                .thenThrow(new RegistrationNotFoundException(55L));

        mockMvc.perform(delete("/api/registrations/event/55")
                        .with(SecurityMockMvcRequestPostProcessors.user("member@example.com").roles("MEMBER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Registration Not Found"));
    }
}
