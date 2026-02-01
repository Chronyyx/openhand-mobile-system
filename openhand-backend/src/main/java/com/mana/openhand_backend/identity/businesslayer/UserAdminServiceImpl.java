package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.Gender;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateUserProfileRequest;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.security.services.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private final RefreshTokenService refreshTokenService;
    private final com.mana.openhand_backend.registrations.businesslayer.RegistrationService registrationService;

    public UserAdminServiceImpl(UserRepository userRepository, AuditLogService auditLogService,
            HttpServletRequest request, RefreshTokenService refreshTokenService,
            com.mana.openhand_backend.registrations.businesslayer.RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.request = request;
        this.refreshTokenService = refreshTokenService;
        this.registrationService = registrationService;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "email"));
    }

    @Override
    public User updateUserRoles(Long userId, Set<String> roles) {
        Set<String> normalizedRoles;
        try {
            normalizedRoles = RoleUtils.normalizeRoles(roles);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException(ex.getMessage());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String oldRoles = String.join(",", user.getRoles());
        String newRolesStr = String.join(",", normalizedRoles);

        user.setRoles(normalizedRoles);
        User updatedUser = userRepository.save(user);

        // Audit Log
        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        String ip = request.getRemoteAddr();
        String agent = request.getHeader("User-Agent");

        auditLogService.logRoleChange(
                user.getId(),
                user.getEmail(),
                oldRoles,
                newRolesStr,
                changedBy,
                ip,
                agent,
                "ADMIN_CONSOLE");

        return updatedUser;
    }

    @Override
    @Transactional
    public User updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getEmail() != null) {
            String trimmedEmail = request.getEmail().trim();
            if (trimmedEmail.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be blank");
            }
            userRepository.findByEmail(trimmedEmail)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Email is already in use");
                    });
            user.setEmail(trimmedEmail);
        }

        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }

        if (request.getPhoneNumber() != null) {
            String normalizedPhone = request.getPhoneNumber().replaceAll("[^0-9+]", "");
            if (normalizedPhone.isBlank()) {
                user.setPhoneNumber(null);
            } else {
                userRepository.findByPhoneNumber(normalizedPhone)
                        .filter(existing -> !existing.getId().equals(userId))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Phone number is already in use");
                        });
                user.setPhoneNumber(normalizedPhone);
            }
        }

        if (request.getGender() != null) {
            try {
                user.setGender(Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid gender value: " + request.getGender());
            }
        }

        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserStatus(Long userId, MemberStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getMemberStatus() == status) {
            if (status == MemberStatus.INACTIVE) {
                refreshTokenService.deleteByUserId(userId);
                registrationService.cancelRegistrationsForUser(userId,
                        "Registration cancelled due to account deactivation.");
            }
            return user;
        }

        user.setMemberStatus(status);
        user.setStatusChangedAt(LocalDateTime.now());

        if (status == MemberStatus.INACTIVE) {
            refreshTokenService.deleteByUserId(userId);
            registrationService.cancelRegistrationsForUser(userId,
                    "Registration cancelled due to account deactivation.");
        }

        return userRepository.save(user);
    }

    @Override
    public List<String> getAvailableRoles() {
        List<String> roles = new ArrayList<>(RoleUtils.ALLOWED_ROLES);
        roles.sort(String::compareTo);
        return roles;
    }
}
