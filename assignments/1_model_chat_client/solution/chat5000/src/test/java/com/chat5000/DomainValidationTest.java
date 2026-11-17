package com.chat5000;

import com.chat5000.cache.CachedAiChatService;
import com.chat5000.domain.ChatRequest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.validation.validator.Validator;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainValidationTest {

    @Test
    void validatesChatRequestBounds() {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of("chat5000.valkey.enabled", false))) {
            Validator validator = ctx.getBean(Validator.class);

            ChatRequest valid = ChatRequest.builder()
                    .prompt("Explain TLS 1.3 ML-KEM")
                    .model("llama3.2:1b")
                    .temperature(0.7)
                    .maxOutputTokens(512)
                    .build();
            assertTrue(validator.validate(valid).isEmpty());

            ChatRequest invalid = ChatRequest.builder()
                    .prompt("   ")
                    .temperature(3.5)
                    .maxOutputTokens(0)
                    .build();
            Set<ConstraintViolation<ChatRequest>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
            assertEquals(3, violations.size());
        }
    }

    @Test
    void buildsDeterministicCacheKeys() {
        String k1 = CachedAiChatService.buildCacheKey("llama3.2:1b", 0.7, 2048, "Hello");
        String k2 = CachedAiChatService.buildCacheKey("llama3.2:1b", 0.7, 2048, "Hello");
        String k3 = CachedAiChatService.buildCacheKey("qwen2.5-coder:1.5b", 0.7, 2048, "Hello");

        assertEquals(k1, k2);
        assertNotEquals(k1, k3);
    }
}
