package com.mana.openhand_backend.identity;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createUserIfMissing("admin@mana.org", "admin123",
                Set.of(RoleUtils.ROLE_ADMIN, RoleUtils.ROLE_MEMBER));

        createUserIfMissing("member@mana.org", "member123",
                Set.of(RoleUtils.ROLE_MEMBER));

        createUserIfMissing("employee@mana.org", "employee123",
                Set.of(RoleUtils.ROLE_EMPLOYEE));
    }

    private void createUserIfMissing(String email, String rawPassword, Set<String> roles) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
        System.out.printf("User %s initialized%n", email);
    }
}
