package com.mana.openhand_backend.security.jwt;

import com.mana.openhand_backend.security.services.UserDetailsImpl;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthTokenFilter authTokenFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withoutAuthorizationHeader_doesNotUseJwtUtilsOrUserDetailsService()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        authTokenFilter.doFilterInternal(request, response, filterChain);

        // we only care that nothing JWT-related was called
        verifyNoInteractions(jwtUtils, userDetailsService);
        verify(filterChain, times(1))
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void doFilterInternal_withValidBearerToken_setsAuthentication()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer validToken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtils.validateJwtToken("validToken")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("validToken")).thenReturn("user@example.com");

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

        authTokenFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());

        verify(jwtUtils, times(1)).validateJwtToken("validToken");
        verify(jwtUtils, times(1)).getUserNameFromJwtToken("validToken");
        verify(userDetailsService, times(1)).loadUserByUsername("user@example.com");
        verify(filterChain, times(1))
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void doFilterInternal_withInvalidToken_doesNotSetAuthentication()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalidToken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtils.validateJwtToken("invalidToken")).thenReturn(false);

        authTokenFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, times(1)).validateJwtToken("invalidToken");
        verify(jwtUtils, never()).getUserNameFromJwtToken(anyString());
        verifyNoInteractions(userDetailsService);
        verify(filterChain, times(1))
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}
