package com.chat5000.api;

import com.chat5000.cache.ValkeyCacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes live Micrometer metrics and Valkey/TLS status for the Web UI and monitoring tools.
 */
@Controller("/api/v1/metrics")
public class MetricsSummaryController {

    private final MeterRegistry meterRegistry;
    private final ValkeyCacheService valkeyCacheService;

    public MetricsSummaryController(MeterRegistry meterRegistry, ValkeyCacheService valkeyCacheService) {
        this.meterRegistry = meterRegistry;
        this.valkeyCacheService = valkeyCacheService;
    }

    @Get
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valkeyConnected", valkeyCacheService.isValkeyConnected());
        result.put("tlsNamedGroups", System.getProperty("jdk.tls.namedGroups", "default"));
        result.put("cacheHits", sumCounter("chat5000.cache.hits"));
        result.put("cacheMisses", sumCounter("chat5000.cache.misses"));
        result.put("chatRequests", sumCounter("chat5000.chat.requests"));
        result.put("authAccepted", sumCounter("chat5000.auth.accepted"));
        result.put("authRejected", sumCounter("chat5000.auth.rejected"));
        result.put("rateLimitRejected", sumCounter("chat5000.ratelimit.rejected"));
        result.put("promptTokens", sumCounter("chat5000.tokens.prompt"));
        result.put("completionTokens", sumCounter("chat5000.tokens.completion"));
        return result;
    }

    private double sumCounter(String name) {
        return meterRegistry.find(name).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }
}
