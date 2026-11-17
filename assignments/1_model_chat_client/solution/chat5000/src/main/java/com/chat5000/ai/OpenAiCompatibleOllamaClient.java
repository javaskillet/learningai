package com.chat5000.ai;

import com.chat5000.config.AiProperties;
import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatResponse;
import com.chat5000.domain.ChatStreamChunk;
import com.chat5000.domain.ModelInfo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible service class targeting Ollama (default base_url http://localhost:11434).
 * Supports single Q&A, multi-turn conversational sessions, SSE streaming, and model discovery.
 */
public class OpenAiCompatibleOllamaClient implements AiChatClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleOllamaClient.class);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleOllamaClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public ChatResponse askSingle(String prompt, String model, Double temperature, Integer maxOutputTokens) {
        return chatSession(null, List.of(ChatMessage.user(prompt)), model, temperature, maxOutputTokens);
    }

    @Override
    public ChatResponse chatSession(
            String sessionId,
            List<ChatMessage> history,
            String model,
            Double temperature,
            Integer maxOutputTokens
    ) {
        long startNanos = System.nanoTime();
        String resolvedModel = resolveModel(model);
        double resolvedTemp = resolveTemperature(temperature);
        int resolvedMaxTokens = resolveMaxTokens(maxOutputTokens);

        Map<String, Object> payload = buildChatPayload(history, resolvedModel, resolvedTemp, resolvedMaxTokens, false);
        try {
            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer ollama")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String usedModel = root.path("model").asText(resolvedModel);
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .model(usedModel)
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .cached(false)
                    .latencyMs(latencyMs)
                    .build();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiProviderException("Failed to call Ollama OpenAI-compatible endpoint: " + e.getMessage(), e);
        }
    }

    @Override
    public Publisher<ChatStreamChunk> streamChat(
            String sessionId,
            List<ChatMessage> messages,
            String model,
            Double temperature,
            Integer maxOutputTokens
    ) {
        String resolvedModel = resolveModel(model);
        double resolvedTemp = resolveTemperature(temperature);
        int resolvedMaxTokens = resolveMaxTokens(maxOutputTokens);

        return Flux.create(sink -> {
            long startNanos = System.nanoTime();
            try {
                Map<String, Object> payload = buildChatPayload(messages, resolvedModel, resolvedTemp, resolvedMaxTokens, true);
                String jsonBody = objectMapper.writeValueAsString(payload);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(normalizeBaseUrl() + "/v1/chat/completions"))
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer ollama")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    sink.error(new AiProviderException("Ollama streaming returned HTTP " + response.statusCode()));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean doneSent = false;
                    while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                        if (!line.startsWith("data:")) {
                            continue;
                        }
                        String data = line.substring(5).trim();
                        if (data.isEmpty()) {
                            continue;
                        }
                        if ("[DONE]".equals(data)) {
                            if (!doneSent) {
                                long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                                sink.next(new ChatStreamChunk(sessionId, resolvedModel, "", true, false, latencyMs));
                                doneSent = true;
                            }
                            break;
                        }
                        JsonNode chunk = objectMapper.readTree(data);
                        String delta = chunk.path("choices").path(0).path("delta").path("content").asText("");
                        String finishReason = chunk.path("choices").path(0).path("finish_reason").asText("");
                        boolean done = "stop".equals(finishReason);
                        if (done) {
                            doneSent = true;
                        }
                        long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                        if (!delta.isEmpty() || done) {
                            sink.next(new ChatStreamChunk(sessionId, resolvedModel, delta, done, false, latencyMs));
                        }
                    }
                    if (!doneSent && !sink.isCancelled()) {
                        long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                        sink.next(new ChatStreamChunk(sessionId, resolvedModel, "", true, false, latencyMs));
                    }
                }
                sink.complete();
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                sink.error(new AiProviderException("Streaming from Ollama failed: " + e.getMessage(), e));
            }
        });
    }

    @Override
    public List<ModelInfo> listModels() {
        try {
            HttpRequest tagsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(tagsRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode modelsNode = root.path("models");
                if (modelsNode.isArray()) {
                    List<ModelInfo> result = new ArrayList<>();
                    for (JsonNode m : modelsNode) {
                        result.add(new ModelInfo(
                                m.path("name").asText(),
                                m.path("size").asLong(0),
                                m.path("details").path("parameter_size").asText("unknown"),
                                m.path("details").path("quantization_level").asText("unknown"),
                                m.path("modified_at").asText("")
                        ));
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to query /api/tags, falling back to /v1/models: {}", e.getMessage());
        }

        try {
            HttpRequest v1Request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/v1/models"))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(v1Request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode data = objectMapper.readTree(response.body()).path("data");
                List<ModelInfo> result = new ArrayList<>();
                if (data.isArray()) {
                    for (JsonNode m : data) {
                        result.add(new ModelInfo(m.path("id").asText(), 0L, "unknown", "unknown", ""));
                    }
                }
                return result;
            }
        } catch (Exception e) {
            LOG.warn("Failed to query /v1/models: {}", e.getMessage());
        }
        return List.of(new ModelInfo(properties.getModel(), 0L, "configured", "default", ""));
    }

    private Map<String, Object> buildChatPayload(
            List<ChatMessage> history,
            String model,
            double temperature,
            int maxOutputTokens,
            boolean stream
    ) {
        List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxOutputTokens);
        payload.put("stream", stream);
        return payload;
    }

    private String normalizeBaseUrl() {
        String base = properties.getBaseUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String resolveModel(String model) {
        return (model != null && !model.isBlank()) ? model : properties.getModel();
    }

    private double resolveTemperature(Double temperature) {
        return temperature != null ? temperature : properties.getTemperature();
    }

    private int resolveMaxTokens(Integer maxOutputTokens) {
        return maxOutputTokens != null ? maxOutputTokens : properties.getMaxOutputTokens();
    }
}
