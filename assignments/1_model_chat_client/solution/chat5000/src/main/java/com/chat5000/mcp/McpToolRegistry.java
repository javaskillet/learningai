package com.chat5000.mcp;

import com.chat5000.cache.ValkeyCacheService;
import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatRequest;
import com.chat5000.domain.ChatResponse;
import com.chat5000.domain.ModelInfo;
import com.chat5000.service.ChatFacade;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GoF Command Registry exposing all Chat5000 REST API capabilities as MCP tools.
 */
@Singleton
public class McpToolRegistry {

    private final ChatFacade chatFacade;
    private final ValkeyCacheService valkeyCacheService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, ToolHandler> handlers = new LinkedHashMap<>();

    @FunctionalInterface
    public interface ToolHandler {
        Object execute(JsonNode arguments) throws Exception;
    }

    public McpToolRegistry(
            ChatFacade chatFacade,
            ValkeyCacheService valkeyCacheService,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        this.chatFacade = chatFacade;
        this.valkeyCacheService = valkeyCacheService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;

        handlers.put("chat_ask", this::handleChatAsk);
        handlers.put("chat_session", this::handleChatSession);
        handlers.put("list_models", args -> Map.of("models", chatFacade.listModels()));
        handlers.put("get_history", this::handleGetHistory);
        handlers.put("clear_history", this::handleClearHistory);
        handlers.put("get_metrics", args -> buildMetricsSummary());
    }

    public List<Map<String, Object>> listTools() {
        return List.of(
                toolDef("chat_ask", "Ask a single question/answer to the local Ollama model (Valkey-cached).",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "prompt", Map.of("type", "string", "description", "User prompt"),
                                        "model", Map.of("type", "string", "description", "Optional Ollama model name"),
                                        "temperature", Map.of("type", "number", "description", "Optional sampling temperature (0.0 - 2.0)"),
                                        "maxOutputTokens", Map.of("type", "integer", "description", "Optional max output tokens")
                                ),
                                "required", List.of("prompt")
                        )),
                toolDef("chat_session", "Send a message in a multi-turn conversational chat session persisted in Valkey.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "sessionId", Map.of("type", "string", "description", "Optional session ID (auto-generated if omitted)"),
                                        "prompt", Map.of("type", "string", "description", "User message"),
                                        "model", Map.of("type", "string", "description", "Optional Ollama model name"),
                                        "temperature", Map.of("type", "number", "description", "Optional sampling temperature"),
                                        "maxOutputTokens", Map.of("type", "integer", "description", "Optional max output tokens")
                                ),
                                "required", List.of("prompt")
                        )),
                toolDef("list_models", "List all available local Ollama models for switching.",
                        Map.of("type", "object", "properties", Map.of())),
                toolDef("get_history", "Retrieve conversation history for a sessionId from Valkey.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of("sessionId", Map.of("type", "string", "description", "Session ID")),
                                "required", List.of("sessionId")
                        )),
                toolDef("clear_history", "Clear conversation history for a sessionId in Valkey.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of("sessionId", Map.of("type", "string", "description", "Session ID")),
                                "required", List.of("sessionId")
                        )),
                toolDef("get_metrics", "Get live Chat5000 metrics (cache hits/misses, requests, tokens, Valkey/TLS status).",
                        Map.of("type", "object", "properties", Map.of()))
        );
    }

    public Map<String, Object> callTool(String name, JsonNode arguments) {
        ToolHandler handler = handlers.get(name);
        if (handler == null) {
            return Map.of(
                    "isError", true,
                    "content", List.of(Map.of("type", "text", "text", "Unknown MCP tool: " + name))
            );
        }
        try {
            meterRegistry.counter("chat5000.mcp.tool_calls", "tool", name).increment();
            Object result = handler.execute(arguments);
            String json = objectMapper.writeValueAsString(result);
            return Map.of(
                    "isError", false,
                    "content", List.of(Map.of("type", "text", "text", json))
            );
        } catch (Exception e) {
            return Map.of(
                    "isError", true,
                    "content", List.of(Map.of("type", "text", "text", "Tool execution failed: " + e.getMessage()))
            );
        }
    }

    private ChatResponse handleChatAsk(JsonNode args) {
        String prompt = requireNonBlank(args, "prompt");
        ChatRequest req = ChatRequest.builder()
                .prompt(prompt)
                .model(optionalText(args, "model"))
                .temperature(optionalDouble(args, "temperature"))
                .maxOutputTokens(optionalInt(args, "maxOutputTokens"))
                .build();
        return chatFacade.askSingle(req);
    }

    private ChatResponse handleChatSession(JsonNode args) {
        String prompt = requireNonBlank(args, "prompt");
        ChatRequest req = ChatRequest.builder()
                .sessionId(optionalText(args, "sessionId"))
                .prompt(prompt)
                .model(optionalText(args, "model"))
                .temperature(optionalDouble(args, "temperature"))
                .maxOutputTokens(optionalInt(args, "maxOutputTokens"))
                .build();
        return chatFacade.chatSession(req);
    }

    private Map<String, Object> handleGetHistory(JsonNode args) {
        String sessionId = requireNonBlank(args, "sessionId");
        List<ChatMessage> history = chatFacade.getHistory(sessionId);
        return Map.of("sessionId", sessionId, "messages", history);
    }

    private Map<String, Object> handleClearHistory(JsonNode args) {
        String sessionId = requireNonBlank(args, "sessionId");
        chatFacade.clearHistory(sessionId);
        return Map.of("sessionId", sessionId, "cleared", true);
    }

    private Map<String, Object> buildMetricsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("valkeyConnected", valkeyCacheService.isValkeyConnected());
        summary.put("cacheHits", sumCounter("chat5000.cache.hits"));
        summary.put("cacheMisses", sumCounter("chat5000.cache.misses"));
        summary.put("chatRequests", sumCounter("chat5000.chat.requests"));
        summary.put("mcpToolCalls", sumCounter("chat5000.mcp.tool_calls"));
        return summary;
    }

    private double sumCounter(String name) {
        return meterRegistry.find(name).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private static Map<String, Object> toolDef(String name, String description, Map<String, Object> inputSchema) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", inputSchema
        );
    }

    private static String requireNonBlank(JsonNode args, String field) {
        String val = optionalText(args, field);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + field);
        }
        return val;
    }

    private static String optionalText(JsonNode args, String field) {
        if (args == null || !args.hasNonNull(field)) {
            return null;
        }
        return args.get(field).asText();
    }

    private static Double optionalDouble(JsonNode args, String field) {
        if (args == null || !args.hasNonNull(field)) {
            return null;
        }
        return args.get(field).asDouble();
    }

    private static Integer optionalInt(JsonNode args, String field) {
        if (args == null || !args.hasNonNull(field)) {
            return null;
        }
        return args.get(field).asInt();
    }
}
