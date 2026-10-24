package com.payshield.identity.Repository;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.identity.Entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    public User findByEmail(String email);

    public boolean existsByEmail(String email);

    // Custom query methods can be defined here if needed
}