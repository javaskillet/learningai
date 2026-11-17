package com.chat5000.domain;

/**
 * Metadata for an available local Ollama model.
 */
public record ModelInfo(
        String name,
        long sizeBytes,
        String parameterSize,
        String quantization,
        String modifiedAt
) {
}
