package com.mana.openhand_backend.security.services;

import com.mana.openhand_backend.identity.dataaccesslayer.RefreshToken;
import com.mana.openhand_backend.identity.dataaccesslayer.RefreshTokenRepository;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${openhand.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(Long userId, String userAgent) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUserAgent(userAgent);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenExpiredException(token.getToken(),
                    "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    public void verifyUserAgent(RefreshToken token, String currentUserAgent) {
        if (token.getUserAgent() != null && !token.getUserAgent().equals(currentUserAgent)) {
            // Potential token theft!
            // In a real scenario, we might want to invalidate ALL tokens for this user
            // For now, we just reject this request
            throw new RuntimeException("Refresh token was used with a different User-Agent. Potential theft detected!");
        }
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)));
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken token) {
        // Delete the old token
        refreshTokenRepository.delete(token);

        // Create a new one for the same user and user agent
        return createRefreshToken(token.getUser().getId(), token.getUserAgent());
    }
}
