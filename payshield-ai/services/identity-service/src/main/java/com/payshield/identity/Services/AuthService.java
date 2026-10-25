
package com.payshield.identity.Services;

import com.payshield.identity.Entity.Role;
import com.payshield.identity.Enum.RoleName;
import com.payshield.identity.Entity.User;
import com.payshield.identity.Repository.RoleRepository;
import com.payshield.identity.Repository.UserRepository;
import com.payshield.identity.Security.JwtTokenService;
import com.payshield.identity.dto.request.LoginRequest;
import com.payshield.identity.dto.request.RefreshTokenRequest;
import com.payshield.identity.dto.request.RegisterRequest;
import com.payshield.identity.dto.response.AuthResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        Role defaultRole = roleRepository.findByName(RoleName.ANALYST)
                .orElseThrow(() -> new IllegalStateException(
                        "Default ANALYST role is not configured"
                ));

        User user = new User();
        user.setEmail(email);
        user.setDisplayName(request.getDisplayName());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setEnabled(true);
        user.getRoles().add(defaultRole);

        User savedUser = userRepository.save(user);

        List<String> roles = savedUser.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .toList();

        String accessToken = jwtTokenService.generateAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                roles
        );

        return new AuthResponse(accessToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User account not found"
                ));

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_")
                        ? authority.substring(5)
                        : authority)
                .toList();

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        return new AuthResponse(accessToken);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {

        // Implement once refresh-token persistence, validation,
        // expiration, rotation, and revocation are configured.
        throw new UnsupportedOperationException(
                "Refresh-token flow has not been implemented yet"
        );
    }
}
