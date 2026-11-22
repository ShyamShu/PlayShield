package com.payshield.transaction.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.payshield.transaction.Security.CustomAccessDeniedHandler;
import com.payshield.transaction.Security.CustomJwtAuthenticationConverter;
import com.payshield.transaction.Security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            RestAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler) {

        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // REST APIs use JWT, so CSRF protection is not needed
                // for this stateless bearer-token setup.
                .csrf(csrf -> csrf.disable())
                // No HTTP session is created or maintained.
                .sessionManagement(session
                        -> session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                // Request-level authorization
                .authorizeHttpRequests(auth -> auth
                        // Public health endpoints
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        // Only ADMIN can submit transactions
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/transactions"
                        ).hasRole("ADMIN")
                        // ADMIN, ANALYST and AUDITOR can view transactions
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/transactions",
                                "/api/v1/transactions/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "ANALYST",
                                "AUDITOR"
                        )
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // Handle authentication and authorization errors
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )
                // Configure this application as a JWT resource server
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public CustomJwtAuthenticationConverter jwtAuthenticationConverter() {
        return new CustomJwtAuthenticationConverter();
    }
}
