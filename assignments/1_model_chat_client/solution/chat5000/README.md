# Chat5000

Local-first AI chat platform built with **Java 25**, **Micronaut 5**, **Gradle 9**, **Ollama** (`http://localhost:11434`), and **Valkey**.

## Features
- **Three OAuth2 Bearer-Protected Modes** on **HTTPS port 9443** (TLS 1.3 + PQC `X25519MLKEM768` + `TLS_AES_256_GCM_SHA384` / `TLS_CHACHA20_POLY1305_SHA256`):
  1. **Web UI** (`https://localhost:9443/`) with streaming output, conversation history, and model switching.
  2. **REST API** (`/api/v1/chat/ask`, `/api/v1/chat/session`, `/api/v1/chat/stream`, `/api/v1/chat/history/{sessionId}`, `/api/v1/models`, `/api/v1/metrics`).
  3. **MCP Server (Streamable HTTP)** at `/mcp` exposing `chat_ask`, `chat_session`, `list_models`, `get_history`, `clear_history`, and `get_metrics`.
- **Valkey Ephemeral Store**: Caches deterministic single Q&A responses, stores multi-turn conversation history, and backs distributed rate limiting.
- **Zero Spring**: Verified by `./gradlew verifyNoSpring`.

## Running Locally
1. Start Valkey:
   ```powershell
   docker compose up -d
   ```
2. Ensure Ollama is running on `http://localhost:11434` (e.g., `ollama serve`).
3. Run Chat5000:
   ```powershell
   .\gradlew run
   ```
4. Open `https://localhost:9443/` and sign in using `POST /oauth/token` (`chat5000-client` / `chat5000-secret`).

## Testing
```powershell
.\gradlew check
```
Uses ephemeral Testcontainers (`valkey/valkey:8-alpine` and `python:3.12-alpine` OpenAI-compatible stub) with zero mock objects.
