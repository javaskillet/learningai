package com.chat5000.ai;

/**
 * Exception thrown when the upstream OpenAI-compatible AI provider fails or returns an error.
 */
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
