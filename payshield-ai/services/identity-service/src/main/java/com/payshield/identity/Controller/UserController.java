
package com.payshield.identity.Controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payshield.identity.Entity.User;
import com.payshield.identity.Enum.RoleName;
import com.payshield.identity.Services.UserService;
import com.payshield.identity.dto.request.UpdateUserRequest;
import com.payshield.identity.dto.response.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/v1/users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(UserResponse::from)
                .toList();

        return ResponseEntity.ok(users);
    }

    // GET /api/v1/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID id) {

        User user = userService.getUserById(id);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    // GET /api/v1/users/by-email?email=user@example.com
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(
            @RequestParam String email) {

        User user = userService.getUserByEmail(email);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    // PATCH /api/v1/users/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        User updatedUser = userService.updateUser(
                id,
                request.getEmail(),
                request.getDisplayName()
        );

        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    // PATCH /api/v1/users/{id}/status?enabled=false
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {

        User updatedUser =
                userService.setUserEnabled(id, enabled);

        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    // PUT /api/v1/users/{id}/roles/ADMIN
    @PutMapping("/{id}/roles/{roleName}")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID id,
            @PathVariable RoleName roleName) {

        User updatedUser = userService.assignRole(id, roleName);

        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    // DELETE /api/v1/users/{id}/roles/ANALYST
    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable UUID id,
            @PathVariable RoleName roleName) {

        User updatedUser = userService.removeRole(id, roleName);

        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    // DELETE /api/v1/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
