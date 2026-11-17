package com.chat5000;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests using ephemeral Valkey and OpenAI-compatible Ollama containers (no mocks).
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Chat5000ContainerIT {

    private static final String OLLAMA_STUB_SCRIPT = """
            import json, http.server
            class H(http.server.BaseHTTPRequestHandler):
                def log_message(self, *args): pass
                def do_GET(self):
                    if self.path == '/api/tags':
                        body = json.dumps({'models': [
                            {'name': 'llama3.2:1b', 'size': 1300000000, 'details': {'parameter_size': '1B', 'quantization_level': 'Q4_0'}, 'modified_at': '2026-01-01T00:00:00Z'},
                            {'name': 'qwen2.5-coder:1.5b', 'size': 986000000, 'details': {'parameter_size': '1.5B', 'quantization_level': 'Q4_K_M'}, 'modified_at': '2026-01-01T00:00:00Z'}
                        ]}).encode()
                        self.send_response(200)
                        self.send_header('Content-Type', 'application/json')
                        self.end_headers()
                        self.wfile.write(body)
                    else:
                        self.send_response(404); self.end_headers()
                def do_POST(self):
                    ln = int(self.headers.get('Content-Length', 0))
                    req = json.loads(self.rfile.read(ln).decode())
                    msgs = req.get('messages', [])
                    last = msgs[-1]['content'] if msgs else ''
                    model = req.get('model', 'llama3.2:1b')
                    if req.get('stream'):
                        self.send_response(200)
                        self.send_header('Content-Type', 'text/event-stream')
                        self.end_headers()
                        for part in ['Local ', 'Ollama ', 'Reply']:
                            chunk = json.dumps({'choices': [{'delta': {'content': part}, 'finish_reason': None}]})
                            self.wfile.write(f'data: {chunk}\\n\\n'.encode())
                            self.wfile.flush()
                        self.wfile.write(b'data: [DONE]\\n\\n')
                        self.wfile.flush()
                    else:
                        reply = f'[{model}] turns={len(msgs)} echo={last}'
                        body = json.dumps({
                            'model': model,
                            'choices': [{'message': {'role': 'assistant', 'content': reply}}],
                            'usage': {'prompt_tokens': 12, 'completion_tokens': 8}
                        }).encode()
                        self.send_response(200)
                        self.send_header('Content-Type', 'application/json')
                        self.end_headers()
                        self.wfile.write(body)
            http.server.ThreadingHTTPServer(('0.0.0.0', 11434), H).serve_forever()
            """;

    @Container
    static final GenericContainer<?> VALKEY = new GenericContainer<>(DockerImageName.parse("valkey/valkey:8-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Container
    static final GenericContainer<?> OLLAMA_CONTAINER = new GenericContainer<>(DockerImageName.parse("python:3.12-alpine"))
            .withExposedPorts(11434)
            .withCommand("python3", "-c", OLLAMA_STUB_SCRIPT)
            .waitingFor(Wait.forHttp("/api/tags").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(30)));

    private static EmbeddedServer server;
    private static HttpClient httpsClient;
    private static ObjectMapper objectMapper;
    private static String baseUrl;
    private static String bearerToken;

    @BeforeAll
    static void startServer() throws Exception {
        String valkeyUri = "redis://" + VALKEY.getHost() + ":" + VALKEY.getMappedPort(6379);
        String ollamaUrl = "http://" + OLLAMA_CONTAINER.getHost() + ":" + OLLAMA_CONTAINER.getMappedPort(11434);

        server = ApplicationContext.run(EmbeddedServer.class, Map.of(
                "micronaut.server.ssl.port", 0,
                "micronaut.server.ssl.key-store.path", "file:src/main/resources/keystore/chat5000-dev.p12",
                "chat5000.valkey.enabled", true,
                "chat5000.valkey.uri", valkeyUri,
                "chat5000.ai.base-url", ollamaUrl,
                "chat5000.rate-limit.requests-per-minute", 30
        ));
        baseUrl = "https://localhost:" + server.getPort();
        objectMapper = server.getApplicationContext().getBean(ObjectMapper.class);
        httpsClient = createTrustAllTls13Client();

        bearerToken = fetchBearerToken();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    @Order(1)
    void enforcesTls13WithPqcCompliantCipherAndServesUi() throws Exception {
        HttpResponse<String> response = httpsClient.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Chat5000"));

        SSLSession sslSession = response.sslSession().orElseThrow();
        assertEquals("TLSv1.3", sslSession.getProtocol());
        assertTrue(
                sslSession.getCipherSuite().equals("TLS_AES_256_GCM_SHA384")
                        || sslSession.getCipherSuite().equals("TLS_CHACHA20_POLY1305_SHA256"),
                "Unexpected cipher suite: " + sslSession.getCipherSuite()
        );
    }

    @Test
    @Order(2)
    void protectsRestAndMcpEndpointsWithOAuthBearer() throws Exception {
        HttpResponse<String> restUnauth = httpsClient.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/models")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, restUnauth.statusCode());

        HttpResponse<String> mcpUnauth = httpsClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/mcp"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, mcpUnauth.statusCode());
    }

    @Test
    @Order(3)
    void singleQuestionUsesValkeyCacheOnSecondCall() throws Exception {
        String body = "{\"prompt\":\"What is Valkey?\",\"model\":\"llama3.2:1b\"}";

        HttpResponse<String> first = authedPost("/api/v1/chat/ask", body);
        assertEquals(200, first.statusCode());
        JsonNode firstJson = objectMapper.readTree(first.body());
        assertFalse(firstJson.get("cached").asBoolean());
        assertTrue(firstJson.get("content").asText().contains("What is Valkey?"));

        HttpResponse<String> second = authedPost("/api/v1/chat/ask", body);
        assertEquals(200, second.statusCode());
        JsonNode secondJson = objectMapper.readTree(second.body());
        assertTrue(secondJson.get("cached").asBoolean(), "Second identical call must hit Valkey cache");
    }

    @Test
    @Order(4)
    void conversationalSessionPersistsHistoryInValkeyAndSupportsModelSwitch() throws Exception {
        String sessionId = "it-session-1";
        HttpResponse<String> turn1 = authedPost("/api/v1/chat/session",
                "{\"sessionId\":\"" + sessionId + "\",\"prompt\":\"Hello\",\"model\":\"llama3.2:1b\"}");
        assertEquals(200, turn1.statusCode());
        assertTrue(objectMapper.readTree(turn1.body()).get("content").asText().contains("turns=1"));

        HttpResponse<String> turn2 = authedPost("/api/v1/chat/session",
                "{\"sessionId\":\"" + sessionId + "\",\"prompt\":\"Switch model\",\"model\":\"qwen2.5-coder:1.5b\"}");
        assertEquals(200, turn2.statusCode());
        String turn2Content = objectMapper.readTree(turn2.body()).get("content").asText();
        assertTrue(turn2Content.contains("turns=3"));
        assertTrue(turn2Content.contains("qwen2.5-coder:1.5b"));

        HttpResponse<String> history = authedGet("/api/v1/chat/history/" + sessionId);
        assertEquals(200, history.statusCode());
        assertEquals(4, objectMapper.readTree(history.body()).get("messages").size());

        HttpResponse<String> cleared = httpsClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/v1/chat/history/" + sessionId))
                        .header("Authorization", "Bearer " + bearerToken)
                        .DELETE().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, cleared.statusCode());
    }

    @Test
    @Order(5)
    void streamsSseChunksAndListsModels() throws Exception {
        HttpResponse<String> models = authedGet("/api/v1/models");
        assertEquals(200, models.statusCode());
        assertTrue(models.body().contains("llama3.2:1b"));
        assertTrue(models.body().contains("qwen2.5-coder:1.5b"));

        HttpResponse<String> stream = authedPost("/api/v1/chat/stream",
                "{\"prompt\":\"Stream test\",\"stream\":true}");
        assertEquals(200, stream.statusCode());
        assertTrue(stream.body().contains("Local "));
        assertTrue(stream.body().contains("Ollama "));
        assertTrue(stream.body().contains("Reply"));
    }

    @Test
    @Order(6)
    void validatesRequestsAndExposesMcpStreamableHttpTools() throws Exception {
        HttpResponse<String> invalid = authedPost("/api/v1/chat/ask", "{\"prompt\":\"\"}");
        assertEquals(400, invalid.statusCode());

        HttpResponse<String> init = authedPost("/mcp",
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        assertEquals(200, init.statusCode());
        String mcpSession = init.headers().firstValue("Mcp-Session-Id").orElse(null);
        assertNotNull(mcpSession);

        HttpResponse<String> listTools = authedPost("/mcp",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        assertEquals(200, listTools.statusCode());
        assertTrue(listTools.body().contains("chat_ask"));
        assertTrue(listTools.body().contains("chat_session"));
        assertTrue(listTools.body().contains("list_models"));

        HttpResponse<String> callAsk = authedPost("/mcp",
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"chat_ask\",\"arguments\":{\"prompt\":\"MCP hello\"}}}");
        assertEquals(200, callAsk.statusCode());
        assertTrue(callAsk.body().contains("MCP hello"));
    }

    @Test
    @Order(7)
    void exposesMetricsAndEnforcesRateLimit() throws Exception {
        HttpResponse<String> metrics = authedGet("/api/v1/metrics");
        assertEquals(200, metrics.statusCode());
        JsonNode m = objectMapper.readTree(metrics.body());
        assertTrue(m.get("valkeyConnected").asBoolean());
        assertTrue(m.get("cacheHits").asDouble() >= 1.0);

        boolean hit429 = false;
        for (int i = 0; i < 35; i++) {
            HttpResponse<String> r = authedGet("/api/v1/models");
            if (r.statusCode() == 429) {
                hit429 = true;
                assertTrue(r.headers().firstValue("Retry-After").isPresent());
                break;
            }
        }
        assertTrue(hit429, "Expected HTTP 429 when exceeding 30 RPM rate limit");
    }

    private static String fetchBearerToken() throws Exception {
        HttpResponse<String> res = httpsClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/oauth/token"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"grant_type\":\"client_credentials\",\"client_id\":\"chat5000-client\",\"client_secret\":\"chat5000-secret\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, res.statusCode());
        return objectMapper.readTree(res.body()).get("access_token").asText();
    }

    private HttpResponse<String> authedGet(String path) throws Exception {
        return httpsClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + bearerToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> authedPost(String path, String jsonBody) throws Exception {
        return httpsClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + bearerToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static HttpClient createTrustAllTls13Client() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, trustAll, new SecureRandom());
        SSLParameters params = new SSLParameters();
        params.setProtocols(new String[]{"TLSv1.3"});
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .sslParameters(params)
                .build();
    }
}
