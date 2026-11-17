# Chat5000 Implementation Plan

## 1. Overview & Objectives
Construct **chat5000** in `c:\java\project\chat5000` using **Java 25**, **Micronaut 5.x**, and **Gradle 9.x** (with zero Spring libraries). The application provides a local-first AI chat platform backed by **Ollama** (`http://localhost:11434` OpenAI-compatible API) and **Valkey** (ephemeral cache & session/rate-limit store), proving local AI is feasible, free, fast, private/secure, load-bearing, and horizontally scalable.

## 2. Core Architectural Baseline
- **Runtime & Build**: JDK 25 (`JavaLanguageVersion.of(25)`), Gradle 9.7.0, Micronaut Platform `5.2.6` (`io.micronaut.application` `5.0.2`).
- **Zero Spring**: No `org.springframework.*` dependencies anywhere in the dependency graph.
- **Zero Active CVEs & Permissive Licensing**: All runtime dependencies verified via Sonatype (`Apache-2.0`, `MIT`, `BSD-3-Clause`, `EPL-2.0`).
- **HTTPS & PQC-Compliant TLS (Port 9443)**:
  - Netty HTTPS server on port `9443` with PKCS12 keystore (`EC` P-384).
  - TLS 1.3 only (`TLSv1.3`), PQC hybrid key exchange enabled via JDK 25 `jdk.tls.namedGroups=X25519MLKEM768,x25519,secp384r1`.
  - Strong 256-bit AEAD ciphers: `TLS_AES_256_GCM_SHA384`, `TLS_CHACHA20_POLY1305_SHA256`.
- **OAuth Bearer Token Security**:
  - `POST /oauth/token` (`grant_type=client_credentials`) issues signed JWTs (`HS512` via Nimbus JOSE+JWT).
  - `BearerTokenFilter` enforces `Authorization: Bearer <token>` across all three modes: Web UI APIs, REST API (`/api/v1/**`), MCP Streamable HTTP (`/mcp/**`), and metrics (`/api/v1/metrics`, `/prometheus`).

## 3. Three-Mode Interface
1. **Web UI** (`/`): Modern, responsive single-page application served over HTTPS:9443 with OAuth Bearer login, model switcher, single Q&A vs. conversational session toggle, SSE streaming output, conversation history sidebar, and live metrics/PQC-TLS indicators.
2. **REST API** (`/api/v1/**`):
   - `POST /api/v1/chat/ask`: Single question/answer (cached in Valkey).
   - `POST /api/v1/chat/session`: Multi-turn conversational chat with history stored in Valkey.
   - `POST /api/v1/chat/stream`: Server-Sent Events (`text/event-stream`) streaming chat completions.
   - `GET /api/v1/chat/history/{sessionId}` & `DELETE /api/v1/chat/history/{sessionId}`: Session history management.
   - `GET /api/v1/models`: List available Ollama models for switching.
   - `GET /api/v1/metrics`: JSON metrics summary (Micrometer counters/timers).
3. **MCP Server (Streamable HTTP at `/mcp`)**:
   - Supports `POST /mcp` (JSON-RPC 2.0 `initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`), `GET /mcp` (SSE stream), and `DELETE /mcp` (session termination) with `Mcp-Session-Id`.
   - Exposes full parity with REST API via tools: `chat_ask`, `chat_session`, `list_models`, `get_history`, `clear_history`, `get_metrics`.

## 4. GoF Design Patterns
- **Strategy**: `AiChatClient` interface with `OpenAiCompatibleOllamaClient` implementation.
- **Factory**: `AiClientFactory` creating/configuring the AI client from `AiProperties`.
- **Decorator**: `CachedAiChatService` wrapping `AiChatClient` with Valkey response caching and Micrometer metrics.
- **Facade**: `ChatFacade` coordinating validation, session history, caching, streaming, and model switching.
- **Builder**: Fluent builders on `ChatRequest`, `ChatResponse`, and `ChatMessage` records.
- **Command / Registry**: `McpToolRegistry` mapping MCP tool names to executable command handlers.

## 5. Caching, Rate Limiting, Validation, & Metrics
- **Valkey Caching**: Lettuce (`io.lettuce:lettuce-core`) connects to Valkey for deterministic Q&A response caching (SHA-256 cache key over model + temperature + maxOutputTokens + prompt), conversation history lists, and distributed rate-limit buckets.
- **Rate Limiting**: `RateLimitFilter` + `RateLimiterService` enforces per-client token-bucket limits and returns `429 Too Many Requests` with `Retry-After`.
- **Request Validation**: `jakarta.validation` annotations (`@NotBlank`, `@Size`, `@DecimalMin`, `@DecimalMax`, `@Min`, `@Max`) with `ValidationExceptionHandler`.
- **Metrics**: Micronaut Micrometer (`io.micrometer`) recording request counts, latency timers, Valkey cache hits/misses, token counts, and rate-limit rejections.

## 6. Testing Strategy (No Mocks — Ephemeral Containers)
- **Unit Tests**: Pure Java tests for domain builders, validation, OAuth JWT issuance/verification, rate limiter, and MCP JSON-RPC handling.
- **Container Integration Tests**: JUnit 5 + Testcontainers using:
  - Ephemeral `valkey/valkey:8-alpine` container for caching, sessions, and rate limiting.
  - Ephemeral OpenAI-compatible HTTP container (`mockserver/mockserver` or `wiremock/wiremock`) simulating Ollama `/v1/chat/completions` (non-streaming + SSE streaming) and `/api/tags`.
  - Real Micronaut HTTPS server verifying TLS 1.3 + PQC cipher, OAuth Bearer enforcement, REST API, Valkey caching, rate limiting, validation, metrics, and MCP Streamable HTTP end-to-end.