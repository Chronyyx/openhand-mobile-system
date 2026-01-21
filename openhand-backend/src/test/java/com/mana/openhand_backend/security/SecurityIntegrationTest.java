package com.mana.openhand_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.presentationlayer.payload.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test Case 1: Bad Credentials -> 401 Unauthorized
     * This verifies the fix in GlobalExceptionHandler.
     */
    @Test
    public void whenBadCredentials_thenReturns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("wrongpassword");

        // Note: AuthController throws BadCredentialsException which
        // GlobalExceptionHandler catches
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test Case 2: Unauthenticated Access to Protected Endpoint -> 401 Unauthorized
     */
    @Test
    public void whenUnauthenticatedAccessToProtected_thenReturns401() throws Exception {
        // /api/registrations requires authentication
        mockMvc.perform(get("/api/registrations"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test Case 3: Authenticated but Insufficient Privileges -> 403 Forbidden
     */
    @Test
    @WithMockUser(username = "user", roles = { "MEMBER" })
    public void whenUserAccessAdminEndpoint_thenReturns403() throws Exception {
        // /api/admin/** requires ADMIN role
        // We are mocked as MEMBER
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
