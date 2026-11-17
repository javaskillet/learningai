package com.chat5000.ai;

import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatResponse;
import com.chat5000.domain.ChatStreamChunk;
import com.chat5000.domain.ModelInfo;
import org.reactivestreams.Publisher;

import java.util.List;

/**
 * GoF Strategy interface for interacting with an OpenAI-compatible local AI provider (Ollama).
 */
public interface AiChatClient {

    ChatResponse askSingle(String prompt, String model, Double temperature, Integer maxOutputTokens);

    ChatResponse chatSession(String sessionId, List<ChatMessage> history, String model, Double temperature, Integer maxOutputTokens);

    Publisher<ChatStreamChunk> streamChat(String sessionId, List<ChatMessage> messages, String model, Double temperature, Integer maxOutputTokens);

    List<ModelInfo> listModels();
}
