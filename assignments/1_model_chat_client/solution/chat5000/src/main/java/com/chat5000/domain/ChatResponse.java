package com.chat5000.domain;

import java.time.Instant;

/**
 * Immutable chat response with token usage, cache hit indicator, and latency metrics.
 * Uses GoF Builder pattern.
 */
public record ChatResponse(
        String sessionId,
        String model,
        String content,
        int promptTokens,
        int completionTokens,
        boolean cached,
        long latencyMs,
        Instant timestamp
) {
    public ChatResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public ChatResponse withCached(boolean cached, long latencyMs) {
        return new ChatResponse(sessionId, model, content, promptTokens, completionTokens, cached, latencyMs, timestamp);
    }

    public ChatResponse withSessionId(String sessionId) {
        return new ChatResponse(sessionId, model, content, promptTokens, completionTokens, cached, latencyMs, timestamp);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sessionId;
        private String model;
        private String content;
        private int promptTokens;
        private int completionTokens;
        private boolean cached;
        private long latencyMs;
        private Instant timestamp = Instant.now();

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder promptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public Builder cached(boolean cached) {
            this.cached = cached;
            return this;
        }

        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(sessionId, model, content, promptTokens, completionTokens, cached, latencyMs, timestamp);
        }
    }
}
