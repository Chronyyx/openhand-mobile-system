package com.mana.openhand_backend.identity.presentationlayer.payload;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtResponseTest {

    @Test
    void constructor_setsAllFields_andDefaultTypeIsBearer() {
        // arrange
        String token = "jwt-token";
        Long id = 1L;
        String email = "user@example.com";
        List<String> roles = List.of("ROLE_MEMBER", "ROLE_ADMIN");

        // act
        JwtResponse response = new JwtResponse(token, id, email, roles);

        // assert
        assertEquals(token, response.getToken());
        assertEquals(id, response.getId());
        assertEquals(email, response.getEmail());
        assertEquals(roles, response.getRoles());

        assertEquals("Bearer", response.getType());
    }

    @Test
    void setters_updateFieldsCorrectly() {
        JwtResponse response = new JwtResponse("t1", 1L, "a@a.com", List.of("ROLE_1"));

        response.setToken("new-token");
        response.setType("NewType");
        response.setId(99L);
        response.setEmail("new@example.com");
        response.setRoles(List.of("ROLE_X"));

        assertEquals("new-token", response.getToken());
        assertEquals("NewType", response.getType());
        assertEquals(99L, response.getId());
        assertEquals("new@example.com", response.getEmail());
        assertEquals(List.of("ROLE_X"), response.getRoles());
    }
}
