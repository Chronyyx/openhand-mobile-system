package com.mana.openhand_backend.identity;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

@InjectMocks
private DatabaseSeeder databaseSeeder;

@Test
void run_createsAdminMemberAndEmployee_whenTheyDoNotExist() throws Exception {
    when(userRepository.existsByEmail("admin@mana.org")).thenReturn(false);
    when(userRepository.existsByEmail("member@mana.org")).thenReturn(false);
    when(userRepository.existsByEmail("employee@mana.org")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("hashed");

        databaseSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());

        List<User> saved = userCaptor.getAllValues();
        assertThat(saved)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("admin@mana.org", "member@mana.org", "employee@mana.org");

        saved.stream()
                .filter(u -> u.getEmail().equals("admin@mana.org"))
                .findFirst()
                .ifPresent(admin -> assertThat(admin.getRoles())
                        .containsExactlyInAnyOrder(RoleUtils.ROLE_ADMIN, RoleUtils.ROLE_MEMBER));

        saved.stream()
                .filter(u -> u.getEmail().equals("member@mana.org"))
                .findFirst()
                .ifPresent(member -> assertThat(member.getRoles())
                        .containsExactly(RoleUtils.ROLE_MEMBER));

        saved.stream()
                .filter(u -> u.getEmail().equals("employee@mana.org"))
                .findFirst()
                .ifPresent(employee -> assertThat(employee.getRoles())
                        .containsExactly(RoleUtils.ROLE_EMPLOYEE));
    }

    @Test
    void run_doesNotCreateUsers_whenTheyAlreadyExist() throws Exception {
        when(userRepository.existsByEmail("admin@mana.org")).thenReturn(true);
        when(userRepository.existsByEmail("member@mana.org")).thenReturn(true);
        when(userRepository.existsByEmail("employee@mana.org")).thenReturn(true);

        databaseSeeder.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
