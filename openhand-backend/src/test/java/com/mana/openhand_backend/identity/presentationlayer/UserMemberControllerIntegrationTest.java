package com.mana.openhand_backend.identity.presentationlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.security.services.UserDetailsImpl;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserMemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
        userRepository.deleteAll();
    }

    @Test
    void deactivateEndpoint_setsUserInactive_andReturnsPayload() throws Exception {
        // arrange
        User user = new User();
        user.setEmail("controller@test.com");
        user.setPasswordHash("hash");
        user.setRoles(Set.of("ROLE_MEMBER"));
        user.setMemberStatus(MemberStatus.ACTIVE);
        User saved = userRepository.save(user);

        UserDetailsImpl principal = UserDetailsImpl.build(saved);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        // act
        String json = mockMvc.perform(post("/api/member/account/deactivate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // assert
        var response = objectMapper.readTree(json);
        assertThat(response.get("memberStatus").asText()).isEqualTo("INACTIVE");
        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getMemberStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(reloaded.getStatusChangedAt()).isNotNull();
    }
}
