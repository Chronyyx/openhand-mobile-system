package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateUserRolesRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAdminController.class)
@AutoConfigureMockMvc
class UserAdminControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @MockBean
    private com.mana.openhand_backend.identity.businesslayer.UserAdminService userAdminService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Update roles with empty set -> 400")
    void updateUserRoles_emptyRoles_returnsBadRequest() throws Exception {
        UpdateUserRolesRequest req = new UpdateUserRolesRequest();
        req.setRoles(Set.of());

        mockMvc.perform(put("/api/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Update roles with blank role -> 400")
    void updateUserRoles_blankRole_returnsBadRequest() throws Exception {
        UpdateUserRolesRequest req = new UpdateUserRolesRequest();
        req.setRoles(Set.of(" "));

        mockMvc.perform(put("/api/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update roles requires authentication")
    void updateUserRoles_requiresAdminRole() throws Exception {
        UpdateUserRolesRequest req = new UpdateUserRolesRequest();
        req.setRoles(Set.of("ROLE_MEMBER"));

        mockMvc.perform(put("/api/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
