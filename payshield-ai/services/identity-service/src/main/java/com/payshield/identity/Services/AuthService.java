
package com.payshield.identity.Services;

import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.identity.Config.JwtProperties;
import com.payshield.identity.Entity.RefreshToken;
import com.payshield.identity.Entity.Role;
import com.payshield.identity.Entity.User;
import com.payshield.identity.Enum.RoleName;
import com.payshield.identity.Exception.InvalidCredentialsException;
import com.payshield.identity.Repository.RoleRepository;
import com.payshield.identity.Repository.UserRepository;
import com.payshield.identity.Security.JwtTokenService;
import com.payshield.identity.dto.request.LoginRequest;
import com.payshield.identity.dto.request.RefreshTokenRequest;
import com.payshield.identity.dto.request.RegisterRequest;
import com.payshield.identity.dto.response.AuthResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

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
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setEnabled(true);
        user.getRoles().add(defaultRole);

        User savedUser = userRepository.save(user);

        return issueTokens(savedUser, getRoleNames(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        Authentication authentication;

        try {
        authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );
        } catch (AuthenticationException ex) {
                throw new InvalidCredentialsException();
        }

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

        return issueTokens(user, roles);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {

        // Validate the existing refresh token before rotating it.
        RefreshToken currentToken =
                refreshTokenService.validateRefreshToken(
                        request.getRefreshToken()
                );

        // This method is transactional too, so the lazy User relationship
        // remains accessible while this outer transaction is active.
        User user = currentToken.getUser();

        if (!user.isEnabled()) {
            refreshTokenService.revokeAllUserTokens(user.getId());

            throw new IllegalArgumentException(
                    "This account is disabled"
            );
        }

        // Invalidates the old token and returns its replacement.
        String newRefreshToken =
                refreshTokenService.rotateRefreshToken(
                        request.getRefreshToken()
                );

        List<String> roles = getRoleNames(user);

        String newAccessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        long expiresIn = jwtProperties.getAccessTokenTtlMinutes() * 60;

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                expiresIn
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(
                request.getRefreshToken()
        );
    }

    private AuthResponse issueTokens(User user, List<String> roles) {

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        long expiresIn = jwtProperties.getAccessTokenTtlMinutes() * 60;

        return new AuthResponse(
                accessToken,
                refreshToken,
                expiresIn
        );
    }

    private List<String> getRoleNames(User user) {
        return user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .toList();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
