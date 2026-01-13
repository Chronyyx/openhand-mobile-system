package com.mana.openhand_backend.identity.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.SignupRequest;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.security.jwt.JwtUtils;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private SendGridEmailService sendGridEmailService;

    @Test
    void register_allowsBlankPhoneAndDoesNotBlockOnEmailFailure() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("user@example.com");
        request.setPassword("Secret123!");
        request.setName("User");
        request.setAge(25);
        request.setPhoneNumber(""); // blank phone should be allowed

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(new User());
        doThrow(new RuntimeException("send failure")).when(sendGridEmailService)
                .sendAccountRegistrationConfirmation(anyString(), anyString());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(sendGridEmailService).sendAccountRegistrationConfirmation(eq("user@example.com"), eq("User"));
    }
}
