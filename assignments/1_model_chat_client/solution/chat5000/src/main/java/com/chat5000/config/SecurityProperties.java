package com.chat5000.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("chat5000.security")
public class SecurityProperties {

    @NotBlank
    private String oauthIssuer = "https://localhost:9443";

    @NotBlank
    private String oauthAudience = "chat5000-api";

    @NotBlank
    private String hmacSecret = "chat5000-super-secret-512-bit-hmac-key-for-local-oauth-tokens-change-in-prod";

    @NotBlank
    private String clientId = "chat5000-client";

    @NotBlank
    private String clientSecret = "chat5000-secret";

    @Min(60)
    private long tokenTtlSeconds = 3600;

    public String getOauthIssuer() {
        return oauthIssuer;
    }

    public void setOauthIssuer(String oauthIssuer) {
        this.oauthIssuer = oauthIssuer;
    }

    public String getOauthAudience() {
        return oauthAudience;
    }

    public void setOauthAudience(String oauthAudience) {
        this.oauthAudience = oauthAudience;
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }
}
