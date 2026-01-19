package com.mana.openhand_backend.identity.domainclientlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseModelTest {

    @Test
    void fromEntity_copiesFieldsAndRolesDefensively() {
        User user = new User("user@example.com", "pwd", new HashSet<>(Set.of("ROLE_MEMBER", "ROLE_ADMIN")));
        user.setId(10L);

        UserResponseModel model = UserResponseModel.fromEntity(user);

        assertEquals(10L, model.getId());
        assertEquals("user@example.com", model.getEmail());
        assertTrue(model.getRoles().contains("ROLE_MEMBER"));

        // Mutate source roles after mapping; response should be unaffected
        user.getRoles().clear();
        assertEquals(Set.of("ROLE_MEMBER", "ROLE_ADMIN"), model.getRoles());
    }

    @Test
    void fromEntity_mapsGenderAndProfileFields() {
        User user = new User("member@example.com", "pwd", new HashSet<>(Set.of("ROLE_MEMBER")));
        user.setId(25L);
        user.setName("Member");
        user.setProfileImageUrl("/uploads/profile-pictures/test.png");
        user.setPhoneNumber("555-1234");
        user.setGender(Gender.MALE);
        user.setAge(42);

        UserResponseModel model = UserResponseModel.fromEntity(user);

        assertEquals("Member", model.getName());
        assertEquals("/uploads/profile-pictures/test.png", model.getProfileImageUrl());
        assertEquals("555-1234", model.getPhoneNumber());
        assertEquals("MALE", model.getGender());
        assertEquals(42, model.getAge());
    }

    @Test
    void setters_shouldUpdateFields() {
        UserResponseModel model = new UserResponseModel(1L, "user@example.com", new HashSet<>(), null, null, null, null, null, null, null);
        LocalDateTime changedAt = LocalDateTime.of(2024, 1, 2, 3, 4);

        model.setId(2L);
        model.setEmail("updated@example.com");
        model.setRoles(Set.of("ROLE_ADMIN"));
        model.setName("Updated");
        model.setProfileImageUrl("/uploads/profile-pictures/new.png");
        model.setPhoneNumber("555-7890");
        model.setGender("FEMALE");
        model.setAge(30);
        model.setMemberStatus("INACTIVE");
        model.setStatusChangedAt(changedAt);

        assertEquals(2L, model.getId());
        assertEquals("updated@example.com", model.getEmail());
        assertEquals(Set.of("ROLE_ADMIN"), model.getRoles());
        assertEquals("Updated", model.getName());
        assertEquals("/uploads/profile-pictures/new.png", model.getProfileImageUrl());
        assertEquals("555-7890", model.getPhoneNumber());
        assertEquals("FEMALE", model.getGender());
        assertEquals(30, model.getAge());
        assertEquals("INACTIVE", model.getMemberStatus());
        assertEquals(changedAt, model.getStatusChangedAt());
    }
}
