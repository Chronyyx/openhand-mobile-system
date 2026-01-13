package com.mana.openhand_backend.identity.presentationlayer.payload;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtResponseTest {

    @Test
    void constructor_setsAllFields_andDefaultTypeIsBearer() {
        // arrange
        String token = "jwt-token";
        String refreshToken = "refresh-token";
        Long id = 1L;
        String email = "user@example.com";
        List<String> roles = List.of("ROLE_MEMBER", "ROLE_ADMIN");
        String name = "Test";
        String phoneNumber = "123";
        String gender = "MALE";
        Integer age = 25;

        // act
        JwtResponse response = new JwtResponse(token, refreshToken, id, email, roles, name, phoneNumber, gender, age);

        // assert
        assertEquals(token, response.getToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals(id, response.getId());
        assertEquals(email, response.getEmail());
        assertEquals(roles, response.getRoles());
        assertEquals(name, response.getName());
        assertEquals(phoneNumber, response.getPhoneNumber());
        assertEquals(gender, response.getGender());
        assertEquals(age, response.getAge());

        assertEquals("Bearer", response.getType());
    }

    @Test
    void setters_updateFieldsCorrectly() {
        JwtResponse response = new JwtResponse("t1", "r1", 1L, "a@a.com", List.of("ROLE_1"), "n", "p", "g", 1);

        response.setToken("new-token");
        response.setRefreshToken("new-refresh-token");
        response.setType("NewType");
        response.setId(99L);
        response.setEmail("new@example.com");
        response.setRoles(List.of("ROLE_X"));
        response.setName("New Name");
        response.setPhoneNumber("999");
        response.setGender("FEMALE");
        response.setAge(99);

        assertEquals("new-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("NewType", response.getType());
        assertEquals(99L, response.getId());
        assertEquals("new@example.com", response.getEmail());
        assertEquals(List.of("ROLE_X"), response.getRoles());
        assertEquals("New Name", response.getName());
        assertEquals("999", response.getPhoneNumber());
        assertEquals("FEMALE", response.getGender());
        assertEquals(99, response.getAge());
    }
}
