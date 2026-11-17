package com.chat5000.security;

import com.chat5000.config.SecurityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies OAuth2 Bearer JWT tokens using HS512 (Nimbus JOSE+JWT).
 */
@Singleton
public class OAuthTokenService {

    private static final String DEFAULT_SCOPE = "chat:read chat:write mcp:tools";

    private final SecurityProperties properties;
    private final byte[] signingKey;

    public OAuthTokenService(SecurityProperties properties) {
        this.properties = properties;
        this.signingKey = derive512BitKey(properties.getHmacSecret());
    }

    public boolean validateClientCredentials(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                clientId.getBytes(StandardCharsets.UTF_8),
                properties.getClientId().getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                properties.getClientSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(String subject, String scope) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(properties.getTokenTtlSeconds());
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer(properties.getOauthIssuer())
                    .audience(properties.getOauthAudience())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("scope", (scope == null || scope.isBlank()) ? DEFAULT_SCOPE : scope)
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
            JWSSigner signer = new MACSigner(signingKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign OAuth JWT token", e);
        }
    }

    public Optional<AuthenticatedPrincipal> verifyBearerToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(signingKey);
            if (!signedJWT.verify(verifier)) {
                return Optional.empty();
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                return Optional.empty();
            }
            if (!properties.getOauthIssuer().equals(claims.getIssuer())) {
                return Optional.empty();
            }
            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(properties.getOauthAudience())) {
                return Optional.empty();
            }

            String subject = claims.getSubject();
            String scope = claims.getStringClaim("scope");
            return Optional.of(new AuthenticatedPrincipal(
                    subject != null ? subject : "anonymous",
                    scope != null ? scope : ""
            ));
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    private static byte[] derive512BitKey(String secret) {
        try {
            return MessageDigest.getInstance("SHA-512")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 unavailable", e);
        }
    }

    public record AuthenticatedPrincipal(String subject, String scope) {
        public boolean hasScope(String required) {
            return scope != null && List.of(scope.split("\\s+")).contains(required);
        }
    }
}
