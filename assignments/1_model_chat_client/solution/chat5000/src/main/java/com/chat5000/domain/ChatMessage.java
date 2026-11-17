package com.chat5000.domain;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Immutable chat message record representing a single turn in a conversation.
 */
@Introspected
public record ChatMessage(
        @NotBlank String role,
        @NotBlank String content,
        Instant timestamp
) {
    public ChatMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, Instant.now());
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, Instant.now());
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, Instant.now());
    }
}
