package com.mana.openhand_backend.security.jwt;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private static final String TEST_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 32 bytes
    private static final int TEST_EXPIRATION = 3_600_000; // 1 hour, int not long

    @BeforeEach
    void setup() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        // 👇 int value, matches field type
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", TEST_EXPIRATION);
    }

    private Authentication mockAuth(String username, List<String> roles) {
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(r -> (GrantedAuthority) () -> r)
                .toList();

        User user = new User(username, "password", authorities);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        return auth;
    }

    @Test
    void generateJwtToken_returnsValidJwt() {
        Authentication auth = mockAuth("samuel", List.of("ROLE_ADMIN"));
        String token = jwtUtils.generateJwtToken(auth);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUserNameFromJwtToken_extractsUsernameCorrectly() {
        Authentication auth = mockAuth("samuel", List.of("ROLE_ADMIN"));
        String token = jwtUtils.generateJwtToken(auth);

        String username = jwtUtils.getUserNameFromJwtToken(token);
        assertEquals("samuel", username);
    }

    @Test
    void validateJwtToken_validToken_returnsTrue() {
        Authentication auth = mockAuth("samuel", List.of("ROLE_ADMIN"));
        String token = jwtUtils.generateJwtToken(auth);

        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_expiredToken_returnsFalse() {
        Key signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

        String expiredToken = Jwts.builder()
                .setSubject("samuel")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 5_000)) // already expired
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtils.validateJwtToken(expiredToken));
    }

    @Test
    void validateJwtToken_malformedToken_returnsFalse() {
        assertFalse(jwtUtils.validateJwtToken("NOT_A_JWT_TOKEN"));
    }

    @Test
    void validateJwtToken_unsupportedToken_returnsFalse() {
        String weirdToken = "aaaa.bbbb.cccc"; // syntactically JWT-like, but invalid
        assertFalse(jwtUtils.validateJwtToken(weirdToken));
    }

    @Test
    void validateJwtToken_emptyToken_returnsFalse() {
        assertFalse(jwtUtils.validateJwtToken(""));
    }
}
