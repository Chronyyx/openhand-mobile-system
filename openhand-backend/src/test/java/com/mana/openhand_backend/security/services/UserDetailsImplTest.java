package com.mana.openhand_backend.security.services;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsImplTest {

        @Test
        void build_fromUser_mapsAllFieldsAndRoles() {
                // arrange
                User user = new User(
                                "user@example.com",
                                "hashed-password",
                                Set.of("ROLE_MEMBER", "ROLE_ADMIN"));
                user.setId(1L);
                user.setAccountNonLocked(true);
                user.setName("John Doe");
                user.setPhoneNumber("+1234567890");
                user.setGender(com.mana.openhand_backend.identity.dataaccesslayer.Gender.MALE);
                user.setAge(30);

                // act
                UserDetailsImpl details = UserDetailsImpl.build(user);

                // assert
                assertEquals(1L, details.getId());
                assertEquals("user@example.com", details.getEmail());
                assertEquals("hashed-password", details.getPassword());
                assertTrue(details.isAccountNonLocked());
                assertEquals("John Doe", details.getName());
                assertEquals("+1234567890", details.getPhoneNumber());
                assertEquals("MALE", details.getGender());
                assertEquals(30, details.getAge());

                // authorities -> role names
                Set<String> roles = details.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet());

                assertEquals(Set.of("ROLE_MEMBER", "ROLE_ADMIN"), roles);
        }

        @Test
        void getUsername_returnsEmail() {
                User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
                UserDetailsImpl details = UserDetailsImpl.build(user);

                assertEquals("user@example.com", details.getUsername());
        }

        @Test
        void equals_comparesByIdOnly() {
                UserDetailsImpl d1 = new UserDetailsImpl(1L, "a@example.com", "pwd", true, Collections.emptyList(), "n",
                                "p", "g", 1);
                UserDetailsImpl d2 = new UserDetailsImpl(1L, "b@example.com", "pwd2", false, Collections.emptyList(),
                                "n2", "p2", "g2", 2);
                UserDetailsImpl d3 = new UserDetailsImpl(2L, "a@example.com", "pwd", true, Collections.emptyList(), "n",
                                "p", "g", 1);

                assertEquals(d1, d2);
                assertNotEquals(d1, d3);
        }

        @Test
        void accountAndCredentialsStatusFlags() {
                UserDetailsImpl details = new UserDetailsImpl(1L, "user@example.com", "pwd", true,
                                Collections.emptyList(), "n", "p", "g", 1);

                assertTrue(details.isAccountNonExpired());
                assertTrue(details.isCredentialsNonExpired());
                assertTrue(details.isEnabled());
                assertTrue(details.isAccountNonLocked());
        }
}
