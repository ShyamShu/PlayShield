package com.payshield.identity.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.identity.Entity.Role;

public interface RoleRepository extends JpaRepository<Role, Short> {
    // Custom query methods can be defined here if needed
    

    public Role findByName(String name);
}
