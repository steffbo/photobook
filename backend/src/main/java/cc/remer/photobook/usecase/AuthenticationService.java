package cc.remer.photobook.usecase;

import cc.remer.photobook.adapter.persistence.RefreshTokenRepository;
import cc.remer.photobook.adapter.persistence.UserRepository;
import cc.remer.photobook.adapter.security.JwtTokenProvider;
import cc.remer.photobook.domain.RefreshToken;
import cc.remer.photobook.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final cc.remer.photobook.config.JwtProperties jwtProperties;

    @Transactional
    public AuthResult login(String email, String password) {
        try {
            log.debug("Attempting login for user: {}", email);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            String accessToken = tokenProvider.generateAccessToken(authentication);
            UUID userId = tokenProvider.getUserIdFromToken(accessToken);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

            String refreshTokenValue = tokenProvider.generateRefreshToken(userId);
            Instant expiresAt = Instant.now().plusMillis(jwtProperties.getRefreshExpiration());

            RefreshToken refreshToken = RefreshToken.builder()
                    .token(refreshTokenValue)
                    .userId(userId)
                    .expiresAt(expiresAt)
                    .revoked(false)
                    .build();

            refreshTokenRepository.save(refreshToken);

            log.info("User logged in successfully: {}", email);

            return AuthResult.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenValue)
                    .user(user)
                    .build();
        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        log.debug("Attempting to refresh token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            log.warn("Attempted to use revoked refresh token");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Attempted to use expired refresh token");
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (!tokenProvider.validateToken(refreshTokenValue)) {
            log.warn("Refresh token validation failed");
            throw new InvalidTokenException("Invalid refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(refreshTokenValue);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(userId, user.getEmail());
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);
        Instant newExpiresAt = Instant.now().plusMillis(jwtProperties.getRefreshExpiration());

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .userId(userId)
                .expiresAt(newExpiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info("Token refreshed successfully for user: {}", user.getEmail());

        return AuthResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(user)
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        log.debug("Attempting to logout with refresh token");

        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("User logged out successfully, userId: {}", token.getUserId());
                });
    }

    @Transactional
    public void logoutAllSessions(UUID userId) {
        log.debug("Logging out all sessions for user: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All sessions logged out for user: {}", userId);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        log.debug("Cleaning up expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }

    public static class AuthResult {
        private final String accessToken;
        private final String refreshToken;
        private final User user;

        private AuthResult(String accessToken, String refreshToken, User user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public static AuthResultBuilder builder() {
            return new AuthResultBuilder();
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public User getUser() {
            return user;
        }

        public static class AuthResultBuilder {
            private String accessToken;
            private String refreshToken;
            private User user;

            public AuthResultBuilder accessToken(String accessToken) {
                this.accessToken = accessToken;
                return this;
            }

            public AuthResultBuilder refreshToken(String refreshToken) {
                this.refreshToken = refreshToken;
                return this;
            }

            public AuthResultBuilder user(User user) {
                this.user = user;
                return this;
            }

            public AuthResult build() {
                return new AuthResult(accessToken, refreshToken, user);
            }
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
