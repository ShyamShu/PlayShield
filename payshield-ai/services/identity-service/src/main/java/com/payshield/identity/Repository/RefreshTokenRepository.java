package com.payshield.identity.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payshield.identity.Entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken , UUID> {


    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNull(UUID userId);
    @Modifying
    @Query("""
        UPDATE RefreshToken t
        SET t.revokedAt = :now
        WHERE t.user.id = :userId
        AND t.revokedAt IS NULL
    """)
    int revokeAllActiveTokens(
            @Param("userId") UUID userId,
            @Param("now") OffsetDateTime now
    );

    @Modifying
    @Query("""
        UPDATE RefreshToken t
        SET t.revokedAt = :now
        WHERE t.id = :id
        AND t.revokedAt IS NULL
    """)
    int revokeIfActive(
            @Param("id") UUID id,
            @Param("now") OffsetDateTime now
    );


    
}
