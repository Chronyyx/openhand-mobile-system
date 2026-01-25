package com.mana.openhand_backend.identity.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.businesslayer.ProfilePictureService;
import com.mana.openhand_backend.identity.dataaccesslayer.PasswordResetToken;
import com.mana.openhand_backend.identity.dataaccesslayer.RefreshToken;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.LoginRequest;
import com.mana.openhand_backend.identity.presentationlayer.payload.ResetPasswordRequest;
import com.mana.openhand_backend.identity.presentationlayer.payload.SignupRequest;
import com.mana.openhand_backend.identity.presentationlayer.payload.TokenRefreshRequest;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.security.jwt.JwtUtils;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import com.mana.openhand_backend.security.services.UserDetailsImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PasswordEncoder encoder;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private SendGridEmailService sendGridEmailService;

    @MockBean
    private com.mana.openhand_backend.identity.dataaccesslayer.PasswordResetTokenRepository passwordResetTokenRepository;

    @MockBean
    private ProfilePictureService profilePictureService;

    @MockBean
    private com.mana.openhand_backend.identity.businesslayer.UserMemberService userMemberService;

    @Test
    void login_success_returnsJwtPayload() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        java.util.Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "user@example.com",
                "pwd",
                true,
                authorities,
                "User",
                "123",
                "MALE",
                25,
                null
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        User user = new User();
        user.setId(1L);
        user.setMemberStatus(com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh");
        when(refreshTokenService.createRefreshToken(1L, "JUnit")).thenReturn(refreshToken);

        when(profilePictureService.toPublicUrl(any(String.class), any()))
                .thenReturn("http://localhost/uploads/profile-pictures/pic.png");

        mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_MEMBER"))
                .andExpect(jsonPath("$.profilePictureUrl").value("http://localhost/uploads/profile-pictures/pic.png"));
    }

    @Test
    void login_missingUserAgent_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        java.util.Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "user@example.com",
                "pwd",
                true,
                authorities,
                "User",
                "123",
                "MALE",
                25,
                null
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        User user = new User();
        user.setId(1L);
        user.setMemberStatus(com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Missing User-Agent header"));
    }

    @Test
    void refreshtoken_success_returnsNewTokens() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("refresh");

        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        token.setUser(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("refresh2");
        newToken.setUser(user);

        when(refreshTokenService.findByToken("refresh")).thenReturn(Optional.of(token));
        when(refreshTokenService.verifyExpiration(token)).thenReturn(token);
        doNothing().when(refreshTokenService).verifyUserAgent(eq(token), any());
        when(refreshTokenService.rotateRefreshToken(token)).thenReturn(newToken);
        when(jwtUtils.generateJwtToken(any(Authentication.class))).thenReturn("access2");

        mockMvc.perform(post("/api/auth/refreshtoken")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access2"))
                .andExpect(jsonPath("$.refreshToken").value("refresh2"));
    }

    @Test
    void refreshtoken_missing_returnsForbidden() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("missing");
        when(refreshTokenService.findByToken("missing")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refreshtoken")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error: Refresh token is not in database!"));
    }

    @Test
    void register_whenEmailExists_returnsBadRequest() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");
        request.setName("User");
        request.setAge(25);

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));
    }

    @Test
    void forgotPassword_whenUserMissing_returnsOk() throws Exception {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account exists for this email, a reset code has been sent."));
    }

    @Test
    void resetPassword_whenInvalidEmail_returnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("missing@example.com");
        request.setCode("123456");
        request.setNewPassword("newpass");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Invalid email or code provided."));
    }

    @Test
    void resetPassword_whenExpired_returnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@example.com");
        request.setCode("123456");
        request.setNewPassword("newpass");

        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().minusMinutes(5));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Invalid or expired code"));
    }
}
