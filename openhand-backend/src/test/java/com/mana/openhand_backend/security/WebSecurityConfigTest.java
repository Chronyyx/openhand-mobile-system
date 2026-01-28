package com.mana.openhand_backend.security;

import com.mana.openhand_backend.security.jwt.AuthEntryPointJwt;
import com.mana.openhand_backend.security.jwt.AuthTokenFilter;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSecurityConfigTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private AuthEntryPointJwt authEntryPointJwt;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private HttpSecurity httpSecurity;

    private WebSecurityConfig webSecurityConfig;

    @BeforeEach
    void setUp() {
        webSecurityConfig = new WebSecurityConfig();
        // inject @Autowired fields and @Value field
        ReflectionTestUtils.setField(webSecurityConfig, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(webSecurityConfig, "unauthorizedHandler", authEntryPointJwt);
        ReflectionTestUtils.setField(webSecurityConfig, "allowedOrigins",
                "http://localhost:3000, https://example.com");
    }

    @Test
    void authenticationJwtTokenFilter_returnsNewFilterInstance() {
        AuthTokenFilter filter = webSecurityConfig.authenticationJwtTokenFilter();
        assertNotNull(filter);
    }

    @Test
    void passwordEncoder_returnsBCryptPasswordEncoderAndEncodes() {
        PasswordEncoder encoder = webSecurityConfig.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);

        String raw = "password";
        String encoded = encoder.encode(raw);
        assertTrue(encoder.matches(raw, encoded));
    }

    @Test
    void authenticationProvider_usesInjectedUserDetailsServiceAndPasswordEncoder() {
        DaoAuthenticationProvider provider = webSecurityConfig.authenticationProvider();

        assertNotNull(provider);

        Object uds = ReflectionTestUtils.getField(provider, "userDetailsService");
        Object pe = ReflectionTestUtils.getField(provider, "passwordEncoder");

        assertSame(userDetailsService, uds);
        assertNotNull(pe);
    }

    @Test
    void authenticationManager_delegatesToAuthenticationConfiguration() throws Exception {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(manager);

        AuthenticationManager result = webSecurityConfig.authenticationManager(authenticationConfiguration);

        assertSame(manager, result);
        verify(authenticationConfiguration, times(1)).getAuthenticationManager();
    }

    @Test
    void corsConfigurationSource_buildsConfigFromAllowedOrigins() {
        CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        assertEquals(
                List.of("http://localhost:3000", "https://example.com"),
                config.getAllowedOriginPatterns());
        assertEquals(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                config.getAllowedMethods());
        assertEquals(List.of("*"), config.getAllowedHeaders());
        assertEquals(List.of("Authorization", "Content-Type"), config.getExposedHeaders());
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
    }

    @Test
    void corsConfigurationSource_whenAllowedOriginsEmpty_throwsIllegalStateException() {
        ReflectionTestUtils.setField(webSecurityConfig, "allowedOrigins", "");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> webSecurityConfig.corsConfigurationSource());

        assertTrue(ex.getMessage().contains("CORS_ALLOWED_ORIGINS"));
    }

    @Test
    void corsConfigurationSource_whenAllowedOriginsNull_throwsIllegalStateException() {
        ReflectionTestUtils.setField(webSecurityConfig, "allowedOrigins", null);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> webSecurityConfig.corsConfigurationSource());

        assertTrue(ex.getMessage().contains("CORS_ALLOWED_ORIGINS"));
    }

    @Test
    void filterChain_configuresHttpSecurityAndBuildsSecurityFilterChain() throws Exception {
        // stub fluent API on HttpSecurity
        when(httpSecurity.cors(any())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.authenticationProvider(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), eq(UsernamePasswordAuthenticationFilter.class)))
                .thenReturn(httpSecurity);

        SecurityFilterChain mockChain = mock(SecurityFilterChain.class);
        when(httpSecurity.build()).thenAnswer(invocation -> mockChain);

        SecurityFilterChain result = webSecurityConfig.filterChain(httpSecurity);

        assertSame(mockChain, result);

        verify(httpSecurity).cors(any());
        verify(httpSecurity).csrf(any());
        verify(httpSecurity).exceptionHandling(any());
        verify(httpSecurity).sessionManagement(any());
        verify(httpSecurity).authorizeHttpRequests(any());
        verify(httpSecurity).authenticationProvider(any());
        verify(httpSecurity)
                .addFilterBefore(any(AuthTokenFilter.class), eq(UsernamePasswordAuthenticationFilter.class));
        verify(httpSecurity).build();
    }

}
