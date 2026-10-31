
package com.payshield.identity.Config;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final String KEY_ID = "payshield-identity-rsa-1";

    @Bean
    public RSAPrivateKey jwtPrivateKey(
            @Value("${JWT_PRIVATE_KEY}") String pem) throws Exception {

        String base64 = extractPemContent(
                pem,
                "-----BEGIN PRIVATE KEY-----",
                "-----END PRIVATE KEY-----"
        );

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(base64)
        );

        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(keySpec);
    }

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
    public JwtEncoder jwtEncoder(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey) {

        // Check that this private key can sign a challenge
        // which the configured public key can verify.
        try {
            byte[] challenge = "PayShield-RSA-key-pair-check"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);

            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(challenge);
            byte[] signedChallenge = signer.sign();

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(challenge);

            if (!verifier.verify(signedChallenge)) {
                throw new IllegalStateException(
                        "JWT public and private keys do not match");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to validate JWT RSA key pair", ex);
        }

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(KEY_ID)
                .build();

        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(new JWKSet(rsaKey));

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            JwtProperties properties) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        var issuerValidator =
                JwtValidators.createDefaultWithIssuer(
                        properties.getIssuer());

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
                                null));
            }

            return OAuth2TokenValidatorResult.success();
        });

        return decoder;
    }

    private static String extractPemContent(
            String pem,
            String beginMarker,
            String endMarker) {

        // Supports a single-line environment value containing literal \n.
        String normalized = pem.replace("\\n", "\n").trim();

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
