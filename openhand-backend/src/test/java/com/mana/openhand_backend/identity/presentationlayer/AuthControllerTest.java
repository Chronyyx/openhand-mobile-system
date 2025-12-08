package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.JwtResponse;
import com.mana.openhand_backend.identity.presentationlayer.payload.LoginRequest;
import com.mana.openhand_backend.identity.presentationlayer.payload.MessageResponse;
import com.mana.openhand_backend.identity.presentationlayer.payload.SignupRequest;
import com.mana.openhand_backend.security.jwt.JwtUtils;
import com.mana.openhand_backend.security.services.UserDetailsImpl;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    UserRepository userRepository;

    @Mock
    UserDetailsServiceImpl userDetailsService;

    @Mock
    PasswordEncoder encoder;

    @Mock
    JwtUtils jwtUtils;

    @InjectMocks
    AuthController authController;

    @Test
    void authenticateUser_whenCredentialsAreValid_returnsJwtResponseAndResetsFailedAttempts() {
        // arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getUsername()).thenReturn("user@example.com");

        // return a non-null collection of authorities (content doesn't really matter here)
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList((GrantedAuthority) () -> "ROLE_MEMBER");
        doReturn(authorities).when(userDetails).getAuthorities();

        // act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof JwtResponse);

        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertEquals("jwt-token", jwtResponse.getToken());
        assertEquals(1L, jwtResponse.getId());
        assertEquals("user@example.com", jwtResponse.getEmail());
        // we only care that roles were mapped, not their exact type/impl
        assertEquals(Collections.singletonList("ROLE_MEMBER"), jwtResponse.getRoles());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils, times(1)).generateJwtToken(authentication);
        verify(userDetailsService, times(1)).resetFailedAttempts("user@example.com");
        verifyNoMoreInteractions(authenticationManager, jwtUtils, userDetailsService);
    }

    @Test
    void authenticateUser_whenBadCredentials_incrementsFailedAttemptsAndPropagatesException() {
        // arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        User user = mock(User.class);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        // act + assert
        assertThrows(BadCredentialsException.class,
                () -> authController.authenticateUser(loginRequest));

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail("user@example.com");
        verify(userDetailsService, times(1)).increaseFailedAttempts(user);
        verifyNoMoreInteractions(authenticationManager, userRepository, userDetailsService);
    }

    @Test
    void registerUser_whenEmailAlreadyExists_returnsBadRequest() {
        // arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("existing@example.com");
        signupRequest.setPassword("password");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse message = (MessageResponse) response.getBody();
        assertEquals("Error: Email is already in use!", message.getMessage());

        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(encoder);
    }

    @Test
    void registerUser_whenRolesNull_assignsDefaultRoleAndSavesUser() {
        // arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("new@example.com");
        signupRequest.setPassword("password");
        // roles remain null

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("password")).thenReturn("encoded-pass");

        // act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        assertEquals("User registered successfully!",
                ((MessageResponse) response.getBody()).getMessage());

        verify(userRepository, times(1)).existsByEmail("new@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(encoder, times(1)).encode("password");
        verifyNoMoreInteractions(userRepository, encoder);
    }

    @Test
    void registerUser_whenRolesContainAdmin_savesUserWithAdminRole() {
        // arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("admin@example.com");
        signupRequest.setPassword("password");
        signupRequest.setRoles(Set.of("admin"));

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(encoder.encode("password")).thenReturn("encoded-pass");

        // act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        assertEquals("User registered successfully!",
                ((MessageResponse) response.getBody()).getMessage());

        verify(userRepository, times(1)).existsByEmail("admin@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(encoder, times(1)).encode("password");
        verifyNoMoreInteractions(userRepository, encoder);
    }
}
