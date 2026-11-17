package com.chat5000.mcp;

import tools.jackson.databind.JsonNode;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.sse.Event;
import jakarta.annotation.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server implementing the Streamable HTTP transport (/mcp) with full REST API tool parity.
 * Protected by OAuth Bearer authentication via {@link com.chat5000.security.BearerTokenFilter}.
 */
@Controller("/mcp")
public class McpController {

    private static final String MCP_SESSION_HEADER = "Mcp-Session-Id";
    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final McpToolRegistry toolRegistry;
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    public McpController(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Post
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM})
    public HttpResponse<?> handlePost(
            HttpRequest<?> httpRequest,
            @Nullable @Header(MCP_SESSION_HEADER) String sessionId,
            @Body JsonNode rpcRequest
    ) {
        String method = rpcRequest.path("method").asText("");
        JsonNode idNode = rpcRequest.get("id");
        boolean isNotification = (idNode == null || idNode.isNull());

        if ("notifications/initialized".equals(method) || isNotification) {
            return HttpResponse.status(HttpStatus.ACCEPTED);
        }

        String resolvedSessionId = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();
        activeSessions.add(resolvedSessionId);

        Object result;
        Map<String, Object> error = null;

        switch (method) {
            case "initialize" -> result = Map.of(
                    "protocolVersion", PROTOCOL_VERSION,
                    "capabilities", Map.of("tools", Map.of("listChanged", false)),
                    "serverInfo", Map.of("name", "chat5000-mcp", "version", "1.0.0")
            );
            case "ping" -> result = Map.of();
            case "tools/list" -> result = Map.of("tools", toolRegistry.listTools());
            case "tools/call" -> {
                JsonNode params = rpcRequest.path("params");
                String toolName = params.path("name").asText("");
                JsonNode arguments = params.path("arguments");
                result = toolRegistry.callTool(toolName, arguments);
            }
            default -> {
                result = null;
                error = Map.of("code", -32601, "message", "Method not found: " + method);
            }
        }

        Map<String, Object> rpcResponse = new LinkedHashMap<>();
        rpcResponse.put("jsonrpc", "2.0");
        rpcResponse.put("id", idNode);
        if (error != null) {
            rpcResponse.put("error", error);
        } else {
            rpcResponse.put("result", result);
        }

        return HttpResponse.ok(rpcResponse)
                .header(MCP_SESSION_HEADER, resolvedSessionId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    @Get
    @Produces(MediaType.TEXT_EVENT_STREAM)
    public Publisher<Event<Map<String, Object>>> handleGet(@Nullable @Header(MCP_SESSION_HEADER) String sessionId) {
        String resolved = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
        activeSessions.add(resolved);
        return Flux.just(Event.of(Map.<String, Object>of(
                "type", "mcp.ready",
                "sessionId", resolved,
                "protocolVersion", PROTOCOL_VERSION
        )).name("ready"));
    }

    @Delete
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> handleDelete(@Nullable @Header(MCP_SESSION_HEADER) String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
        return HttpResponse.ok(Map.of("closed", true, "sessionId", sessionId != null ? sessionId : ""));
    }
}
