package com.mana.openhand_backend.identity.businesslayer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.security.services.RefreshTokenService;

@Service
public class UserMemberServiceImpl implements UserMemberService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final com.mana.openhand_backend.registrations.businesslayer.RegistrationService registrationService;

    public UserMemberServiceImpl(UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            com.mana.openhand_backend.registrations.businesslayer.RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.registrationService = registrationService;
    }

    @Override
    public User getProfile(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public User getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId,
            com.mana.openhand_backend.identity.presentationlayer.payload.ProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.gender() != null) {
            try {
                user.setGender(com.mana.openhand_backend.identity.dataaccesslayer.Gender.valueOf(request.gender()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid gender value: " + request.gender(), e);
            }
        }
        if (request.age() != null) {
            Integer age = request.age();
            if (age >= 13 && age <= 120) {
                user.setAge(age);
            }
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User deactivateAccount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getMemberStatus() == MemberStatus.INACTIVE) {
            refreshTokenService.deleteByUserId(userId);
            registrationService.cancelRegistrationsForUser(userId,
                    "Registration cancelled due to account deactivation.");
            return user;
        }

        user.setMemberStatus(MemberStatus.INACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());

        // Revoke all refresh tokens to ensure any active sessions are closed.
        refreshTokenService.deleteByUserId(userId);
        registrationService.cancelRegistrationsForUser(userId,
                "Registration cancelled due to account deactivation.");

        return userRepository.save(user);
    }
}
