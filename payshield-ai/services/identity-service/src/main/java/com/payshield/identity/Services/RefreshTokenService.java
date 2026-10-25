
package com.payshield.identity.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.identity.Entity.RefreshToken;
import com.payshield.identity.Entity.User;
import com.payshield.identity.Repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Generates a random refresh token, stores only its SHA-256 hash,
     * and returns the raw token to the caller.
     */
    @Transactional
    public String createRefreshToken(User user) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(
                OffsetDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Validates a refresh token and returns its database record.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            throw new IllegalArgumentException(
                    "Refresh token is expired or revoked"
            );
        }

        return refreshToken;
    }

    /**
     * Rotates a valid token. The old token is revoked and linked
     * to the newly persisted replacement token.
     */
    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken oldToken = validateRefreshToken(rawToken);

        User user = oldToken.getUser();

        // Create and persist the replacement token first.
        String newRawToken = createRefreshToken(user);

        RefreshToken newToken = refreshTokenRepository
                .findByTokenHash(hashToken(newRawToken))
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Replacement refresh token was not persisted"));

        oldToken.revoke();
        oldToken.setReplacedBy(newToken);

        refreshTokenRepository.save(oldToken);

        return newRawToken;
    }

    /**
     * Revokes one refresh token.
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Revokes all non-revoked refresh tokens belonging to a user.
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findAllByUser_IdAndRevokedAtIsNull(userId);

        tokens.forEach(RefreshToken::revoke);

        refreshTokenRepository.saveAll(tokens);
    }

    /**
     * SHA-256 hex encoding produces exactly 64 characters,
     * matching RefreshToken.tokenHash validation.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", ex);
        }
    }
}
