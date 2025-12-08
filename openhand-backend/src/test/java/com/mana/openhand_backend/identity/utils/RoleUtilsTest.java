package com.mana.openhand_backend.identity.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleUtilsTest {

    // ---------- normalizeRolesWithDefault ----------

    @Test
    void normalizeRolesWithDefault_nullRoles_returnsMemberRole() {
        Set<String> result = RoleUtils.normalizeRolesWithDefault(null);

        assertEquals(1, result.size());
        assertTrue(result.contains(RoleUtils.ROLE_MEMBER));
    }

    @Test
    void normalizeRolesWithDefault_emptyRoles_returnsMemberRole() {
        Set<String> result = RoleUtils.normalizeRolesWithDefault(new HashSet<>());

        assertEquals(1, result.size());
        assertTrue(result.contains(RoleUtils.ROLE_MEMBER));
    }

    @Test
    void normalizeRolesWithDefault_validRoles_normalizesAll() {
        Set<String> input = Set.of("admin", " member ", "ROLE_EMPLOYEE");

        Set<String> result = RoleUtils.normalizeRolesWithDefault(input);

        assertEquals(3, result.size());
        assertTrue(result.contains(RoleUtils.ROLE_ADMIN));
        assertTrue(result.contains(RoleUtils.ROLE_MEMBER));
        assertTrue(result.contains(RoleUtils.ROLE_EMPLOYEE));
    }

    // ---------- normalizeRoles (error cases) ----------

    @Test
    void normalizeRoles_null_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RoleUtils.normalizeRoles(null)
        );
        assertTrue(ex.getMessage().contains("At least one role must be provided"));
    }

    @Test
    void normalizeRoles_emptySet_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RoleUtils.normalizeRoles(new HashSet<>())
        );
        assertTrue(ex.getMessage().contains("At least one role must be provided"));
    }

    @Test
    void normalizeRoles_nullOrEmptyStringRole_throwsException() {
        Set<String> roles = Set.of("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RoleUtils.normalizeRoles(roles)
        );
        assertTrue(ex.getMessage().contains("Role value cannot be null or empty"));
    }

    @Test
    void normalizeRoles_unsupportedPrefixedRole_throwsException() {
        Set<String> roles = Set.of("ROLE_MANAGER");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RoleUtils.normalizeRoles(roles)
        );
        assertTrue(ex.getMessage().contains("Unsupported role"));
    }

    @Test
    void normalizeRoles_unsupportedPlainRole_throwsException() {
        Set<String> roles = Set.of("manager");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RoleUtils.normalizeRoles(roles)
        );
        assertTrue(ex.getMessage().contains("Unsupported role"));
    }

    // ---------- normalizeRoles (happy paths, via normalizeSingleRole) ----------

    @Test
    void normalizeRoles_plainNamesAreMappedToConstants_caseInsensitiveAndTrimmed() {
        Set<String> input = Set.of(" admin ", "MEMBER", "eMpLoYeE");

        Set<String> result = RoleUtils.normalizeRoles(input);

        assertEquals(3, result.size());
        assertTrue(result.contains(RoleUtils.ROLE_ADMIN));
        assertTrue(result.contains(RoleUtils.ROLE_MEMBER));
        assertTrue(result.contains(RoleUtils.ROLE_EMPLOYEE));
    }

    @Test
    void normalizeRoles_prefixedRolesAreValidatedAndUppercased() {
        // Valid prefixed roles should just be returned as upper-case
        Set<String> input = Set.of("ROLE_ADMIN", "ROLE_MEMBER", "ROLE_EMPLOYEE");

        Set<String> result = RoleUtils.normalizeRoles(input);

        assertEquals(3, result.size());
        assertTrue(result.contains("ROLE_ADMIN"));
        assertTrue(result.contains("ROLE_MEMBER"));
        assertTrue(result.contains("ROLE_EMPLOYEE"));
    }

    // ---------- ALLOWED_ROLES sanity check ----------

    @Test
    void allowedRoles_containsAllExpectedRoles() {
        assertEquals(3, RoleUtils.ALLOWED_ROLES.size());
        assertTrue(RoleUtils.ALLOWED_ROLES.contains(RoleUtils.ROLE_ADMIN));
        assertTrue(RoleUtils.ALLOWED_ROLES.contains(RoleUtils.ROLE_MEMBER));
        assertTrue(RoleUtils.ALLOWED_ROLES.contains(RoleUtils.ROLE_EMPLOYEE));
    }
}
