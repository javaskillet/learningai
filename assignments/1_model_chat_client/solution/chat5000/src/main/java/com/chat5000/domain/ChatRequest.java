package com.chat5000.domain;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated request payload for single Q&A, conversational sessions, and streaming chat.
 * Uses GoF Builder pattern for fluent construction.
 */
@Introspected
public record ChatRequest(
        @Size(max = 128) String sessionId,
        @NotBlank(message = "Prompt must not be blank")
        @Size(max = 32000, message = "Prompt must not exceed 32000 characters")
        String prompt,
        @Size(max = 128) String model,
        @DecimalMin(value = "0.0", message = "Temperature must be >= 0.0")
        @DecimalMax(value = "2.0", message = "Temperature must be <= 2.0")
        Double temperature,
        @Min(value = 1, message = "maxOutputTokens must be >= 1")
        @Max(value = 32768, message = "maxOutputTokens must be <= 32768")
        Integer maxOutputTokens,
        Boolean stream
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sessionId;
        private String prompt;
        private String model;
        private Double temperature;
        private Integer maxOutputTokens;
        private Boolean stream;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(sessionId, prompt, model, temperature, maxOutputTokens, stream);
        }
    }
}
