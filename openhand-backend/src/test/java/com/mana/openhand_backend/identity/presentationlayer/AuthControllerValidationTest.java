package com.mana.openhand_backend.identity.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.presentationlayer.payload.LoginRequest;
import com.mana.openhand_backend.identity.presentationlayer.payload.SignupRequest;
import com.mana.openhand_backend.identity.presentationlayer.payload.TokenRefreshRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.security.jwt.JwtUtils;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean AuthenticationManager authenticationManager;
    @MockBean UserRepository userRepository;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean PasswordEncoder encoder;
    @MockBean JwtUtils jwtUtils;
    @MockBean RefreshTokenService refreshTokenService;

    @Test
    void login_withMissingEmail_returnsBadRequest() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(" ");
        req.setPassword("pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_withInvalidEmail_returnsBadRequest() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setEmail("not-an-email");
        req.setPassword("pw");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_withBlankToken_returnsBadRequest() throws Exception {
        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken(" ");

        mockMvc.perform(post("/api/auth/refreshtoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
