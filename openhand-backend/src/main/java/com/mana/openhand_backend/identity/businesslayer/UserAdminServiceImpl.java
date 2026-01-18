package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    public UserAdminServiceImpl(UserRepository userRepository, AuditLogService auditLogService,
            HttpServletRequest request) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.request = request;
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
    public List<String> getAvailableRoles() {
        List<String> roles = new ArrayList<>(RoleUtils.ALLOWED_ROLES);
        roles.sort(String::compareTo);
        return roles;
    }
}
