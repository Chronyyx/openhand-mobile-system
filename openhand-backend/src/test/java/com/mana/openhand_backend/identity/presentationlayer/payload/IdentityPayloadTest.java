package com.mana.openhand_backend.identity.presentationlayer.payload;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;

import static org.junit.jupiter.api.Assertions.*;

class IdentityPayloadTest {

    @Test
    void testSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");
        request.setName("Test User");
        request.setAge(25);
        request.setGender(com.mana.openhand_backend.identity.dataaccesslayer.Gender.OTHER);
        request.setPhoneNumber("+1234567890");
        request.setRoles(Set.of("user"));

        assertEquals("test@test.com", request.getEmail());
        assertEquals("password", request.getPassword());
        assertEquals("Test User", request.getName());
        assertEquals(25, request.getAge());
        assertEquals(com.mana.openhand_backend.identity.dataaccesslayer.Gender.OTHER, request.getGender());
        assertEquals("+1234567890", request.getPhoneNumber());
        assertEquals(Set.of("user"), request.getRoles());
    }

    @Test
    void testLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        assertEquals("test@test.com", request.getEmail());
        assertEquals("password", request.getPassword());
    }

    @Test
    void testTokenRefreshRequest() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("refresh-token");

        assertEquals("refresh-token", request.getRefreshToken());
    }

    @Test
    void testJwtResponse() {
        List<String> roles = Collections.singletonList("ROLE_USER");
        JwtResponse response = new JwtResponse("access", "refresh", 1L, "user@test.com", roles, "Test User",
            "1234567890", "MALE", 30, MemberStatus.ACTIVE, LocalDateTime.now());

        // Test Setters if they exist
        response.setToken("access2");
        response.setRefreshToken("refresh2");
        response.setId(2L);
        response.setEmail("user2@test.com");
        response.setRoles(Collections.singletonList("ROLE_ADMIN"));
        response.setType("Bearer");

        assertEquals("access2", response.getToken());
        assertEquals("refresh2", response.getRefreshToken());
        assertEquals(2L, response.getId());
        assertEquals("user2@test.com", response.getEmail());
        assertEquals(Collections.singletonList("ROLE_ADMIN"), response.getRoles());
        assertEquals("Bearer", response.getType());
    }

    @Test
    void testMessageResponse() {
        MessageResponse response = new MessageResponse("Success");
        response.setMessage("New Message");
        assertEquals("New Message", response.getMessage());
    }

    @Test
    void testTokenRefreshResponse() {
        TokenRefreshResponse response = new TokenRefreshResponse("access", "refresh");

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        response.setAccessToken("access2");
        response.setRefreshToken("refresh2");
        response.setTokenType("Basic");

        assertEquals("access2", response.getAccessToken());
        assertEquals("refresh2", response.getRefreshToken());
        assertEquals("Basic", response.getTokenType());
    }

    @Test
    void testUpdateUserRolesRequest() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        Set<String> roles = Set.of("mod");
        request.setRoles(roles);

        assertEquals(roles, request.getRoles());
    }
}
