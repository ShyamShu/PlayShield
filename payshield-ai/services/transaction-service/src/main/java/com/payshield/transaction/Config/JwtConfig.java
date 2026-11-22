
package com.payshield.transaction.Config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;


@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public RSAPublicKey jwtPublicKey(
            @Value("${JWT_PUBLIC_KEY}") String pem) throws Exception {

        String base64 = extractPemContent(
                pem,
                "-----BEGIN PUBLIC KEY-----",
                "-----END PUBLIC KEY-----"
        );

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
                Base64.getDecoder().decode(base64)
        );

        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(keySpec);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            JwtProperties properties) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // Validate issuer, expiration and standard timestamps
        var issuerValidator =
                JwtValidators.createDefaultWithIssuer(
                        properties.getIssuer());

        // Validate audience
        decoder.setJwtValidator(jwt -> {

            var issuerResult = issuerValidator.validate(jwt);

            if (issuerResult.hasErrors()) {
                return issuerResult;
            }

            if (jwt.getAudience() == null
                    || !jwt.getAudience().contains(
                            properties.getAudience())) {

                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error(
                                "invalid_token",
                                "Invalid JWT audience",
                                null
                        )
                );
            }

            return OAuth2TokenValidatorResult.success();
        });

        return decoder;
    }

    private static String extractPemContent(
            String pem,
            String beginMarker,
            String endMarker) {

        // Supports actual newlines and escaped newline characters
        String normalized = pem
                .replace("\\n", "\n")
                .trim();

        if (!normalized.startsWith(beginMarker)
                || !normalized.contains(endMarker)) {

            throw new IllegalArgumentException(
                    "JWT key has an unexpected PEM format. Expected "
                            + beginMarker);
        }

        return normalized
                .replace(beginMarker, "")
                .replace(endMarker, "")
                .replaceAll("\\s", "");
    }
}
