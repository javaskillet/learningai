package com.chat5000.service;

import com.chat5000.ai.AiChatClient;
import com.chat5000.cache.ValkeyCacheService;
import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatRequest;
import com.chat5000.domain.ChatResponse;
import com.chat5000.domain.ChatStreamChunk;
import com.chat5000.domain.ModelInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GoF Facade coordinating AI client calls, Valkey session history, streaming accumulation, and Micrometer metrics.
 */
@Singleton
public class ChatFacade {

    private final AiChatClient aiChatClient;
    private final ValkeyCacheService cacheService;
    private final MeterRegistry meterRegistry;

    public ChatFacade(AiChatClient aiChatClient, ValkeyCacheService cacheService, MeterRegistry meterRegistry) {
        this.aiChatClient = aiChatClient;
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
    }

    public ChatResponse askSingle(ChatRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ChatResponse response = aiChatClient.askSingle(
                    request.prompt(),
                    request.model(),
                    request.temperature(),
                    request.maxOutputTokens()
            );
            recordSuccessMetrics("single", response);
            return response;
        } finally {
            sample.stop(meterRegistry.timer("chat5000.chat.duration", "mode", "single"));
        }
    }

    public ChatResponse chatSession(ChatRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String sessionId = resolveSessionId(request.sessionId());
            List<ChatMessage> history = new ArrayList<>(cacheService.getSessionHistory(sessionId));
            ChatMessage userMessage = ChatMessage.user(request.prompt());
            history.add(userMessage);

            ChatResponse response = aiChatClient.chatSession(
                    sessionId,
                    history,
                    request.model(),
                    request.temperature(),
                    request.maxOutputTokens()
            );

            cacheService.appendSessionMessages(sessionId, List.of(
                    userMessage,
                    ChatMessage.assistant(response.content())
            ));
            recordSuccessMetrics("session", response);
            return response.withSessionId(sessionId);
        } finally {
            sample.stop(meterRegistry.timer("chat5000.chat.duration", "mode", "session"));
        }
    }

    public Publisher<ChatStreamChunk> streamChat(ChatRequest request) {
        boolean hasSession = request.sessionId() != null && !request.sessionId().isBlank();
        String sessionId = hasSession ? request.sessionId() : UUID.randomUUID().toString();
        List<ChatMessage> messages = new ArrayList<>();
        if (hasSession) {
            messages.addAll(cacheService.getSessionHistory(sessionId));
        }
        ChatMessage userMessage = ChatMessage.user(request.prompt());
        messages.add(userMessage);

        StringBuilder accumulated = new StringBuilder();
        meterRegistry.counter("chat5000.chat.requests", "mode", "stream").increment();

        return Flux.from(aiChatClient.streamChat(
                        sessionId,
                        messages,
                        request.model(),
                        request.temperature(),
                        request.maxOutputTokens()))
                .doOnNext(chunk -> {
                    if (chunk.delta() != null) {
                        accumulated.append(chunk.delta());
                    }
                })
                .doOnComplete(() -> {
                    if (hasSession && !accumulated.isEmpty()) {
                        cacheService.appendSessionMessages(sessionId, List.of(
                                userMessage,
                                ChatMessage.assistant(accumulated.toString())
                        ));
                    }
                });
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return cacheService.getSessionHistory(sessionId);
    }

    public void clearHistory(String sessionId) {
        cacheService.clearSessionHistory(sessionId);
    }

    public List<ModelInfo> listModels() {
        return aiChatClient.listModels();
    }

    private void recordSuccessMetrics(String mode, ChatResponse response) {
        meterRegistry.counter("chat5000.chat.requests", "mode", mode).increment();
        if (response.completionTokens() > 0) {
            meterRegistry.counter("chat5000.tokens.completion").increment(response.completionTokens());
        }
        if (response.promptTokens() > 0) {
            meterRegistry.counter("chat5000.tokens.prompt").increment(response.promptTokens());
        }
    }

    private static String resolveSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
    }
}
