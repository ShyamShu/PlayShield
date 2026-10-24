package com.payshield.identity.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.identity.Entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken , UUID> {


    public RefreshToken findBytokenHash(String token);
    
}
