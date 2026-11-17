package com.chat5000.cache;

import com.chat5000.ai.AiChatClient;
import com.chat5000.config.AiProperties;
import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatResponse;
import com.chat5000.domain.ChatStreamChunk;
import com.chat5000.domain.ModelInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Primary;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * GoF Decorator around {@link AiChatClient} that caches single Q&A responses in Valkey
 * and records Micrometer cache/latency metrics.
 */
@Singleton
@Primary
public class CachedAiChatService implements AiChatClient {

    private final AiChatClient delegate;
    private final ValkeyCacheService cacheService;
    private final AiProperties aiProperties;
    private final MeterRegistry meterRegistry;

    public CachedAiChatService(
            @Named("ollama") AiChatClient delegate,
            ValkeyCacheService cacheService,
            AiProperties aiProperties,
            MeterRegistry meterRegistry
    ) {
        this.delegate = delegate;
        this.cacheService = cacheService;
        this.aiProperties = aiProperties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ChatResponse askSingle(String prompt, String model, Double temperature, Integer maxOutputTokens) {
        long startNanos = System.nanoTime();
        String effectiveModel = (model != null && !model.isBlank()) ? model : aiProperties.getModel();
        double effectiveTemp = temperature != null ? temperature : aiProperties.getTemperature();
        int effectiveMaxTokens = maxOutputTokens != null ? maxOutputTokens : aiProperties.getMaxOutputTokens();

        String key = buildCacheKey(effectiveModel, effectiveTemp, effectiveMaxTokens, prompt);
        Optional<ChatResponse> cached = cacheService.getCachedResponse(key);
        if (cached.isPresent()) {
            meterRegistry.counter("chat5000.cache.hits", "model", effectiveModel).increment();
            long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            return cached.get().withCached(true, latencyMs);
        }

        meterRegistry.counter("chat5000.cache.misses", "model", effectiveModel).increment();
        ChatResponse response = delegate.askSingle(prompt, effectiveModel, effectiveTemp, effectiveMaxTokens);
        cacheService.putCachedResponse(key, response);
        return response;
    }

    @Override
    public ChatResponse chatSession(
            String sessionId,
            List<ChatMessage> history,
            String model,
            Double temperature,
            Integer maxOutputTokens
    ) {
        return delegate.chatSession(sessionId, history, model, temperature, maxOutputTokens);
    }

    @Override
    public Publisher<ChatStreamChunk> streamChat(
            String sessionId,
            List<ChatMessage> messages,
            String model,
            Double temperature,
            Integer maxOutputTokens
    ) {
        return delegate.streamChat(sessionId, messages, model, temperature, maxOutputTokens);
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    public static String buildCacheKey(String model, double temperature, int maxOutputTokens, String prompt) {
        String raw = model + "|" + temperature + "|" + maxOutputTokens + "|" + prompt;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
