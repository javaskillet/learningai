package com.chat5000.domain;

/**
 * Streaming SSE chunk emitted during real-time model token generation.
 */
public record ChatStreamChunk(
        String sessionId,
        String model,
        String delta,
        boolean done,
        boolean cached,
        long latencyMs
) {
}
