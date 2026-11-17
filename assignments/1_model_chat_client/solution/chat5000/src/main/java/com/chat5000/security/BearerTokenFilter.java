package com.chat5000.security;

import com.chat5000.domain.ApiError;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.Set;

/**
 * Enforces OAuth2 Bearer token authentication across REST API, MCP Streamable HTTP, and metrics endpoints.
 */
@Filter("/**")
public class BearerTokenFilter implements HttpServerFilter {

    public static final String ATTR_SUBJECT = "chat5000.auth.subject";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/",
            "/index.html",
            "/styles.css",
            "/app.js",
            "/favicon.ico",
            "/oauth/token",
            "/health"
    );

    private final OAuthTokenService tokenService;
    private final MeterRegistry meterRegistry;

    public BearerTokenFilter(OAuthTokenService tokenService, MeterRegistry meterRegistry) {
        this.tokenService = tokenService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String path = request.getPath();
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return chain.proceed(request);
        }

        String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            meterRegistry.counter("chat5000.auth.rejected", "reason", "missing_token").increment();
            return Publishers.just(unauthorized(path, "Missing or invalid Authorization: Bearer <token> header"));
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        Optional<OAuthTokenService.AuthenticatedPrincipal> principal = tokenService.verifyBearerToken(token);
        if (principal.isEmpty()) {
            meterRegistry.counter("chat5000.auth.rejected", "reason", "invalid_token").increment();
            return Publishers.just(unauthorized(path, "Bearer token is invalid or expired"));
        }

        request.setAttribute(ATTR_SUBJECT, principal.get().subject());
        meterRegistry.counter("chat5000.auth.accepted").increment();
        return chain.proceed(request);
    }

    private static MutableHttpResponse<ApiError> unauthorized(String path, String message) {
        return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"chat5000\", error=\"invalid_token\"")
                .body(ApiError.of(401, "unauthorized", message, path));
    }
}
