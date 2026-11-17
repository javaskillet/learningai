package com.chat5000.security;

import com.chat5000.config.SecurityProperties;
import com.chat5000.domain.ApiError;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * OAuth2 client_credentials token endpoint for obtaining Bearer JWTs.
 */
@Controller("/oauth")
public class OAuthTokenController {

    private final OAuthTokenService tokenService;
    private final SecurityProperties securityProperties;

    public OAuthTokenController(OAuthTokenService tokenService, SecurityProperties securityProperties) {
        this.tokenService = tokenService;
        this.securityProperties = securityProperties;
    }

    @Post("/token")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> token(HttpRequest<?> request, @Body Map<String, String> body) {
        String grantType = body != null ? body.get("grant_type") : null;
        if (!"client_credentials".equals(grantType)) {
            return HttpResponse.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of(400, "unsupported_grant_type",
                            "Only grant_type=client_credentials is supported", request.getPath()));
        }

        String clientId = body.get("client_id");
        String clientSecret = body.get("client_secret");

        String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if ((clientId == null || clientSecret == null) && authHeader != null && authHeader.startsWith("Basic ")) {
            String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6).trim()), StandardCharsets.UTF_8);
            int idx = decoded.indexOf(':');
            if (idx > 0) {
                clientId = decoded.substring(0, idx);
                clientSecret = decoded.substring(idx + 1);
            }
        }

        if (!tokenService.validateClientCredentials(clientId, clientSecret)) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of(401, "invalid_client",
                            "Invalid client_id or client_secret", request.getPath()));
        }

        String scope = body.getOrDefault("scope", "chat:read chat:write mcp:tools");
        String jwt = tokenService.issueToken(clientId, scope);
        return HttpResponse.ok(Map.of(
                "access_token", jwt,
                "token_type", "Bearer",
                "expires_in", securityProperties.getTokenTtlSeconds(),
                "scope", scope
        ));
    }
}
