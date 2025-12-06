package com.mana.openhand_backend.security.services;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        return UserDetailsImpl.build(user);
    }

    @Transactional
    public void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempt() + 1;
        user.setFailedAttempt(newFailAttempts);
        if (newFailAttempts >= 20) {
            user.setAccountNonLocked(false);
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedAttempt() > 0) {
                user.setFailedAttempt(0);
                userRepository.save(user);
            }
        });
    }
}