
package com.payshield.identity.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.identity.Entity.RefreshToken;
import com.payshield.identity.Entity.User;
import com.payshield.identity.Exception.InvalidRefreshTokenException;
import com.payshield.identity.Repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevocationService revocationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenRevocationService revocationService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revocationService = revocationService;
    }

    @Transactional
    public String createRefreshToken(User user) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(
                OffsetDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS)
        );

        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public RefreshToken validateRefreshToken(String rawToken) {
        RefreshToken token = findToken(rawToken);

        // A replacement link indicates that this token was rotated.
        if (token.getReplacedBy() != null) {
            revokeAllAfterReuse(token.getUser().getId());
        }

        if (!token.isActive()) {
            throw new IllegalArgumentException(
                    "Refresh token is expired or revoked"
            );
        }

        return token;
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken oldToken = findToken(rawToken);

        UUID userId = oldToken.getUser().getId();

        // Detect replay of a token that was already rotated.
        if (oldToken.getReplacedBy() != null) {
            revokeAllAfterReuse(userId);
        }

        if (!oldToken.isActive()) {
            throw new IllegalArgumentException(
                    "Refresh token is expired or revoked"
            );
        }

        // Only one concurrent request can change this row.
        int updated = refreshTokenRepository.revokeIfActive(
                oldToken.getId(),
                OffsetDateTime.now()
        );

        if (updated != 1) {
            // Another request may have rotated this token first.
            revokeAllAfterReuse(userId);
        }

        // Synchronize the managed entity with the bulk database update.
        oldToken.revoke();

        // The replacement is created in the same transaction.
        // If creation fails, the old-token revocation rolls back too.
        String newRawToken = createRefreshToken(oldToken.getUser());

        RefreshToken newToken = refreshTokenRepository
                .findByTokenHash(hashToken(newRawToken))
                .orElseThrow(() -> new IllegalStateException(
                        "Replacement refresh token was not persisted"
                ));

        oldToken.setReplacedBy(newToken);
        refreshTokenRepository.save(oldToken);

        return newRawToken;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    }
                });
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        revocationService.revokeAllUserTokens(userId);
    }

    private RefreshToken findToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        return refreshTokenRepository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> invalidToken("NOT_FOUND"));
    }

    private void revokeAllAfterReuse(UUID userId) {
      

        // Separate bean, REQUIRES_NEW transaction.
        // Revocation commits before the exception is thrown.
        revocationService.revokeAllUserTokens(userId);

        throw new InvalidRefreshTokenException();
    }

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
                    "SHA-256 is unavailable", ex
            );
        }
    }
    private InvalidRefreshTokenException invalidToken(String reason) {
        return new InvalidRefreshTokenException();
    }
}
