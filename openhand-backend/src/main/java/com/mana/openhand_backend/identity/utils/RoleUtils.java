package com.mana.openhand_backend.identity.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoleUtils {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MEMBER = "ROLE_MEMBER";
    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";

    public static final Set<String> ALLOWED_ROLES = Set.of(ROLE_ADMIN, ROLE_MEMBER, ROLE_EMPLOYEE);

    private RoleUtils() {
    }

    public static Set<String> normalizeRolesWithDefault(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>(Collections.singleton(ROLE_MEMBER));
        }
        return normalizeRoles(roles);
    }

    public static Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be provided.");
        }

        Set<String> normalized = roles.stream()
                .map(RoleUtils::normalizeSingleRole)
                .collect(Collectors.toCollection(HashSet::new));

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be provided.");
        }

        return normalized;
    }

    private static String normalizeSingleRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role value cannot be null or empty.");
        }

        String trimmed = role.trim();
        if (trimmed.startsWith("ROLE_")) {
            String upper = trimmed.toUpperCase();
            if (!ALLOWED_ROLES.contains(upper)) {
                throw new IllegalArgumentException("Unsupported role: " + role);
            }
            return upper;
        }

        switch (trimmed.toLowerCase()) {
            case "admin":
                return ROLE_ADMIN;
            case "member":
                return ROLE_MEMBER;
            case "employee":
                return ROLE_EMPLOYEE;
            default:
                throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }
}
