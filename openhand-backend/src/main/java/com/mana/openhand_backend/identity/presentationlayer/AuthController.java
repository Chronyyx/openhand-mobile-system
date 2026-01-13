package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.RefreshToken;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.*;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mana.openhand_backend.security.jwt.JwtUtils;
import com.mana.openhand_backend.security.services.InvalidRefreshTokenException;
import com.mana.openhand_backend.security.services.RefreshTokenService;
import com.mana.openhand_backend.security.services.UserDetailsImpl;
import com.mana.openhand_backend.security.services.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    SendGridEmailService sendGridEmailService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String username = loginRequest.getEmail();
        logger.info("Login attempt received.");
        if (username != null && !username.contains("@")) {
            // Standardized log message to avoid leaking user existence
            logger.info("Login attempt with phone number.");

            // Normalize phone number (strip non-digits, preserve +)
            String normalizedPhoneInput = username.replaceAll("[^0-9+]", "");

            String resolvedEmail = userRepository.findByPhoneNumber(normalizedPhoneInput)
                    .map(User::getEmail)
                    .orElse(null);

            if (resolvedEmail != null) {
                username = resolvedEmail;
            } else {
                // To prevent user enumeration, perform a dummy authentication attempt
                // and always return a generic error message.
                try {
                    // Use a dummy password check to simulate authentication time (bcrypt check)
                    // This mitigates timing attacks by ensuring "user not found" takes similar time
                    // to "bad password"
                    String dummyHash = "$2a$10$wS2a.9.2./.9.3.5.1.4.1.2.3.5.1.2.3.5.1.2.3.5.1.2.3.5."; // Invalid, but
                                                                                                       // structurally
                                                                                                       // correct-ish
                                                                                                       // length
                    encoder.matches(loginRequest.getPassword(), dummyHash);
                } catch (Exception ignored) {
                    // Ignore the result, always return the same error
                }
                // Return generic error immediately to avoid further processing with invalid
                // username
                throw new BadCredentialsException("Bad credentials");
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

            userDetailsService.resetFailedAttempts(username);
        } catch (BadCredentialsException e) {
            logger.warn("Bad credentials for: {}", username);
            String finalUsername = username;
            userRepository.findByEmail(finalUsername).ifPresent(user -> {
                userDetailsService.increaseFailedAttempts(user);
            });
            throw e;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Missing User-Agent header"));
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId(), userAgent);

        return ResponseEntity.ok(new JwtResponse(jwt,
                refreshToken.getToken(),
                userDetails.getId(),
                userDetails.getUsername(),
                roles,
                userDetails.getName(),
                userDetails.getPhoneNumber(),
                userDetails.getGender(),
                userDetails.getAge()));
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request,
            HttpServletRequest httpRequest) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    refreshTokenService.verifyUserAgent(token, httpRequest.getHeader("User-Agent"));
                    return token;
                })
                .map(token -> {
                    // Rotate Token
                    RefreshToken newToken = refreshTokenService.rotateRefreshToken(token);

                    // Generate new JWT
                    // We need to rebuild the Authentication object or just generate token from user
                    // details
                    // Since generateJwtToken needs Authentication, we can create a dummy one or
                    // refactor JwtUtils
                    // For now, let's create a UsernamePasswordAuthenticationToken
                    User user = newToken.getUser();
                    UserDetailsImpl userDetails = UserDetailsImpl.build(user);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    String tokenStr = jwtUtils.generateJwtToken(authentication);

                    return ResponseEntity.ok(new TokenRefreshResponse(tokenStr, newToken.getToken()));
                })
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is not in database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long userId = userDetails.getId();
        refreshTokenService.deleteByUserId(userId);
        return ResponseEntity.ok(new MessageResponse("Log out successful!"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        String normalizedPhone = null;
        if (signUpRequest.getPhoneNumber() != null && !signUpRequest.getPhoneNumber().trim().isEmpty()) {
            normalizedPhone = signUpRequest.getPhoneNumber().replaceAll("[^0-9+]", "");
            if (userRepository.existsByPhoneNumber(normalizedPhone)) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Phone number is already in use!"));
            }
        }

        Set<String> roles;
        try {
            roles = RoleUtils.normalizeRolesWithDefault(signUpRequest.getRoles());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
        }

        // Create new user's account
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(encoder.encode(signUpRequest.getPassword()));
        user.setRoles(roles);
        user.setName(signUpRequest.getName());

        // Normalize phone number before saving
        if (normalizedPhone != null) {
            user.setPhoneNumber(normalizedPhone);
        }

        user.setGender(signUpRequest.getGender());
        user.setAge(signUpRequest.getAge());
        userRepository.save(user);

        try {
            sendGridEmailService.sendAccountRegistrationConfirmation(
                    user.getEmail(),
                    user.getName()
            );
        } catch (Exception ex) {
            logger.error("Failed to send account registration email to {}: {}", user.getEmail(), ex.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
