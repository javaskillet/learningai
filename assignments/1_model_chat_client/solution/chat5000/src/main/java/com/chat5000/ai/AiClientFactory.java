package com.chat5000.ai;

import com.chat5000.config.AiProperties;
import tools.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * GoF Factory creating the configured OpenAI-compatible Ollama AI client.
 */
@Factory
public class AiClientFactory {

    @Singleton
    @Named("ollama")
    public AiChatClient ollamaClient(AiProperties properties, ObjectMapper objectMapper) {
        return new OpenAiCompatibleOllamaClient(properties, objectMapper);
    }
}
