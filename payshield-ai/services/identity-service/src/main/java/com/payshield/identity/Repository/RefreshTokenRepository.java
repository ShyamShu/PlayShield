package com.payshield.identity.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.identity.Entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken , UUID> {


    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNull(UUID userId);


    
}
