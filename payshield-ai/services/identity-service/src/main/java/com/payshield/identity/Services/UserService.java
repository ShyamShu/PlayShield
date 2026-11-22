
package com.payshield.identity.Services;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.identity.Entity.Role;
import com.payshield.identity.Entity.User;
import com.payshield.identity.Enum.RoleName;
import com.payshield.identity.Repository.RoleRepository;
import com.payshield.identity.Repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenService refreshTokenService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenService = refreshTokenService;
    }

    // Get all users
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get a user by ID
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    // Get a user by email
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    // Update a user's profile
    @Transactional
    public User updateUser(
            UUID userId,
            String email,
            String displayName) {

        User user = getUserById(userId);

        String normalizedEmail = normalizeEmail(email);

        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Display name is required");
        }

        if (displayName.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Display name must not exceed 100 characters");
        }

        user.setEmail(normalizedEmail);
        user.setDisplayName(displayName.trim());

        return userRepository.save(user);
    }

    // Enable or disable an account
    @Transactional
    public User setUserEnabled(UUID userId, boolean enabled) {
        User user = getUserById(userId);

        user.setEnabled(enabled);

        User savedUser = userRepository.save(user);

        // Revoke refresh tokens when disabling the account.
        if (!enabled) {
            refreshTokenService.revokeAllUserTokens(userId);
        }

        return savedUser;
    }

    // Assign a role to a user
    @Transactional
    public User assignRole(UUID userId, RoleName roleName) {
        User user = getUserById(userId);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Role not found: " + roleName));

        boolean alreadyHasRole = user.getRoles().stream()
                .anyMatch(existing ->
                        existing.getName() == roleName);

        if (!alreadyHasRole) {
            user.addRole(role);
            userRepository.save(user);
        }

        return user;
    }

    // Remove a role from a user
    @Transactional
    public User removeRole(UUID userId, RoleName roleName) {
        User user = getUserById(userId);

        Role role = user.getRoles().stream()
                .filter(existing ->
                        existing.getName() == roleName)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User does not have role: " + roleName));

        if (user.getRoles().size() <= 1) {
            throw new IllegalStateException(
                    "Cannot remove the user's last role");
        }

        user.removeRole(role);

        return userRepository.save(user);
    }

    // Permanently delete a user
    // Use account disabling instead when audit history must be preserved.
    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUserById(userId);

        // Revoke credentials before deletion.
        refreshTokenService.revokeAllUserTokens(userId);

        userRepository.delete(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
