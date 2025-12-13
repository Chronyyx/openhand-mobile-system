package com.mana.openhand_backend.identity.domainclientlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import org.junit.jupiter.api.Test;

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
}
