package com.mana.openhand_backend.identity;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseSeederTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private DatabaseSeeder seeder;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        seeder = new DatabaseSeeder();

        // inject mocks manually (since field injection is used)
        seeder.userRepository = userRepository;
        seeder.passwordEncoder = passwordEncoder;

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    @Test
    void run_createsAdminAndMember_whenTheyDoNotExist() throws Exception {
        // both users DO NOT exist
        when(userRepository.existsByEmail("admin@mana.org")).thenReturn(false);
        when(userRepository.existsByEmail("member@mana.org")).thenReturn(false);

        seeder.run();

        // capture arguments for both saved users
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());

        var savedUsers = captor.getAllValues();

        // assert Admin user
        User admin = savedUsers.get(0);
        assertEquals("admin@mana.org", admin.getEmail());
        assertEquals("encoded", admin.getPasswordHash());
        assertTrue(admin.getRoles().containsAll(Set.of("ROLE_ADMIN", "ROLE_MEMBER")));

        // assert Member user
        User member = savedUsers.get(1);
        assertEquals("member@mana.org", member.getEmail());
        assertEquals("encoded", member.getPasswordHash());
        assertTrue(member.getRoles().contains("ROLE_MEMBER"));
    }

    @Test
    void run_doesNotCreateUsers_whenTheyAlreadyExist() throws Exception {
        when(userRepository.existsByEmail("admin@mana.org")).thenReturn(true);
        when(userRepository.existsByEmail("member@mana.org")).thenReturn(true);

        seeder.run();

        // verify nothing saved
        verify(userRepository, never()).save(any(User.class));
    }
}
