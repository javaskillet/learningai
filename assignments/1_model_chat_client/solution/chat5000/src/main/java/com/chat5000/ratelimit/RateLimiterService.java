package com.chat5000.ratelimit;

import com.chat5000.cache.ValkeyCacheService;
import com.chat5000.config.RateLimitProperties;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter backed by Valkey sliding/fixed 60-second windows with local atomic fallback.
 */
@Singleton
public class RateLimiterService {

    private static final long WINDOW_SECONDS = 60L;

    private final RateLimitProperties properties;
    private final ValkeyCacheService valkeyCacheService;
    private final Map<String, LocalWindow> localWindows = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties properties, ValkeyCacheService valkeyCacheService) {
        this.properties = properties;
        this.valkeyCacheService = valkeyCacheService;
    }

    public RateLimitResult tryAcquire(String clientKey) {
        if (!properties.isEnabled()) {
            return new RateLimitResult(true, properties.getRequestsPerMinute(), 0L);
        }

        int limit = properties.getRequestsPerMinute();
        long epochMinute = Instant.now().getEpochSecond() / WINDOW_SECONDS;
        String windowKey = clientKey + ":" + epochMinute;

        Optional<Long> valkeyCount = valkeyCacheService.incrementRateLimitWindow(windowKey, WINDOW_SECONDS);
        long currentCount;
        if (valkeyCount.isPresent()) {
            currentCount = valkeyCount.get();
        } else {
            LocalWindow window = localWindows.compute(clientKey, (k, existing) -> {
                if (existing == null || existing.epochMinute.get() != epochMinute) {
                    return new LocalWindow(epochMinute);
                }
                return existing;
            });
            currentCount = window.counter.incrementAndGet();
        }

        if (currentCount <= limit) {
            return new RateLimitResult(true, (int) Math.max(0, limit - currentCount), 0L);
        }
        long retryAfter = WINDOW_SECONDS - (Instant.now().getEpochSecond() % WINDOW_SECONDS);
        return new RateLimitResult(false, 0, Math.max(1L, retryAfter));
    }

    private static final class LocalWindow {
        private final AtomicLong epochMinute;
        private final AtomicInteger counter = new AtomicInteger(0);

        private LocalWindow(long epochMinute) {
            this.epochMinute = new AtomicLong(epochMinute);
        }
    }

    public record RateLimitResult(boolean allowed, int remaining, long retryAfterSeconds) {
    }
}
