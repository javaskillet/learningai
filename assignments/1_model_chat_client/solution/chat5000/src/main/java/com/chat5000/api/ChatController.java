package com.chat5000.api;

import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatRequest;
import com.chat5000.domain.ChatResponse;
import com.chat5000.domain.ChatStreamChunk;
import com.chat5000.domain.ModelInfo;
import com.chat5000.service.ChatFacade;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.sse.Event;
import io.micronaut.validation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for single Q&A, conversational sessions, SSE streaming, history, and model switching.
 */
@Validated
@Controller("/api/v1")
public class ChatController {

    private final ChatFacade chatFacade;

    public ChatController(ChatFacade chatFacade) {
        this.chatFacade = chatFacade;
    }

    @Post("/chat/ask")
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse askSingle(@Valid @Body ChatRequest request) {
        return chatFacade.askSingle(request);
    }

    @Post("/chat/session")
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chatSession(@Valid @Body ChatRequest request) {
        return chatFacade.chatSession(request);
    }

    @Post("/chat/stream")
    @Produces(MediaType.TEXT_EVENT_STREAM)
    public Publisher<Event<ChatStreamChunk>> streamChat(@Valid @Body ChatRequest request) {
        return Flux.from(chatFacade.streamChat(request))
                .map(chunk -> Event.of(chunk).name(chunk.done() ? "done" : "chunk"));
    }

    @Get("/chat/history/{sessionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getHistory(@PathVariable @NotBlank String sessionId) {
        List<ChatMessage> history = chatFacade.getHistory(sessionId);
        return Map.of(
                "sessionId", sessionId,
                "messages", history
        );
    }

    @Delete("/chat/history/{sessionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> clearHistory(@PathVariable @NotBlank String sessionId) {
        chatFacade.clearHistory(sessionId);
        return Map.of(
                "sessionId", sessionId,
                "cleared", true
        );
    }

    @Get("/models")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> listModels() {
        List<ModelInfo> models = chatFacade.listModels();
        return Map.of("models", models);
    }
}
