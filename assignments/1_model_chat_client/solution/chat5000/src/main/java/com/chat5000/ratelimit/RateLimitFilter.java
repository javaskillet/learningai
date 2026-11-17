package com.chat5000.ratelimit;

import com.chat5000.domain.ApiError;
import com.chat5000.security.BearerTokenFilter;
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

/**
 * Enforces per-client rate limiting on API and MCP endpoints, returning HTTP 429 when exceeded.
 */
@Filter({"/api/**", "/mcp/**"})
public class RateLimitFilter implements HttpServerFilter {

    private final RateLimiterService rateLimiterService;
    private final MeterRegistry meterRegistry;

    public RateLimitFilter(RateLimiterService rateLimiterService, MeterRegistry meterRegistry) {
        this.rateLimiterService = rateLimiterService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String subject = request.getAttribute(BearerTokenFilter.ATTR_SUBJECT, String.class)
                .orElseGet(() -> request.getRemoteAddress().getAddress().getHostAddress());

        RateLimiterService.RateLimitResult result = rateLimiterService.tryAcquire(subject);
        if (!result.allowed()) {
            meterRegistry.counter("chat5000.ratelimit.rejected", "subject", subject).increment();
            return Publishers.just(HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()))
                    .header("X-RateLimit-Remaining", "0")
                    .body(ApiError.of(429, "too_many_requests",
                            "Rate limit exceeded. Retry after " + result.retryAfterSeconds() + "s",
                            request.getPath())));
        }

        return Publishers.map(chain.proceed(request), response ->
                response.header("X-RateLimit-Remaining", String.valueOf(result.remaining())));
    }
}
