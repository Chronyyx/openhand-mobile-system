package com.mana.openhand_backend.identity;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
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
        if (!userRepository.existsByEmail("admin@mana.org")) {
            User admin = new User();
            admin.setEmail("admin@mana.org");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_MEMBER");
            admin.setRoles(roles);
            userRepository.save(admin);
            System.out.println("Admin Initialized");
        }

        if (!userRepository.existsByEmail("member@mana.org")) {
            User member = new User();
            member.setEmail("member@mana.org");
            member.setPasswordHash(passwordEncoder.encode("member123"));
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_MEMBER");
            member.setRoles(roles);
            userRepository.save(member);
            System.out.println("Member Initialized");
        }
    }
}