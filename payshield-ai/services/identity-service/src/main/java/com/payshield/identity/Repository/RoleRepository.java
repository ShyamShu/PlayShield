package com.payshield.identity.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.identity.Entity.Role;
import com.payshield.identity.Enum.RoleName;

public interface RoleRepository extends JpaRepository<Role, Short> {
    // Custom query methods can be defined here if needed
    

    public Optional<Role> findByName(RoleName name);
}
