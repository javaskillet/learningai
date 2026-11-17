package com.chat5000.cache;

import com.chat5000.config.ValkeyProperties;
import com.chat5000.domain.ChatMessage;
import com.chat5000.domain.ChatResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral Valkey data store service for AI response caching, conversation history, and rate-limit counters.
 */
@Singleton
public class ValkeyCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(ValkeyCacheService.class);
    private static final String CACHE_PREFIX = "chat5000:cache:";
    private static final String SESSION_PREFIX = "chat5000:session:";
    private static final String RATELIMIT_PREFIX = "chat5000:rl:";

    private final ValkeyProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, List<ChatMessage>> fallbackSessions = new ConcurrentHashMap<>();

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;

    public ValkeyCacheService(ValkeyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (properties.isEnabled()) {
            try {
                this.redisClient = RedisClient.create(properties.getUri());
                this.connection = redisClient.connect();
                LOG.info("Connected to Valkey at {}", properties.getUri());
            } catch (Exception e) {
                LOG.warn("Valkey unavailable at {} ({}). Falling back to local ephemeral store.", properties.getUri(), e.getMessage());
                this.connection = null;
            }
        }
    }

    public boolean isValkeyConnected() {
        if (connection == null || !connection.isOpen()) {
            return false;
        }
        try {
            return "PONG".equalsIgnoreCase(connection.sync().ping());
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<ChatResponse> getCachedResponse(String cacheKey) {
        if (connection == null) {
            return Optional.empty();
        }
        try {
            String json = connection.sync().get(CACHE_PREFIX + cacheKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, ChatResponse.class));
        } catch (Exception e) {
            LOG.warn("Valkey cache read failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void putCachedResponse(String cacheKey, ChatResponse response) {
        if (connection == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            connection.sync().setex(CACHE_PREFIX + cacheKey, properties.getCacheTtlSeconds(), json);
        } catch (Exception e) {
            LOG.warn("Valkey cache write failed: {}", e.getMessage());
        }
    }

    public List<ChatMessage> getSessionHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        if (connection != null) {
            try {
                String json = connection.sync().get(SESSION_PREFIX + sessionId);
                if (json != null) {
                    return objectMapper.readValue(json, new TypeReference<>() {});
                }
                return List.of();
            } catch (Exception e) {
                LOG.warn("Valkey session read failed: {}", e.getMessage());
            }
        }
        return new ArrayList<>(fallbackSessions.getOrDefault(sessionId, List.of()));
    }

    public void appendSessionMessages(String sessionId, List<ChatMessage> newMessages) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        List<ChatMessage> updated = new ArrayList<>(getSessionHistory(sessionId));
        updated.addAll(newMessages);
        if (connection != null) {
            try {
                String json = objectMapper.writeValueAsString(updated);
                connection.sync().setex(SESSION_PREFIX + sessionId, properties.getSessionTtlSeconds(), json);
                return;
            } catch (Exception e) {
                LOG.warn("Valkey session write failed: {}", e.getMessage());
            }
        }
        fallbackSessions.put(sessionId, updated);
    }

    public void clearSessionHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        fallbackSessions.remove(sessionId);
        if (connection != null) {
            try {
                connection.sync().del(SESSION_PREFIX + sessionId);
            } catch (Exception e) {
                LOG.warn("Valkey session delete failed: {}", e.getMessage());
            }
        }
    }

    public Optional<Long> incrementRateLimitWindow(String subjectKey, long windowSeconds) {
        if (connection == null) {
            return Optional.empty();
        }
        try {
            RedisCommands<String, String> sync = connection.sync();
            String key = RATELIMIT_PREFIX + subjectKey;
            Long count = sync.incr(key);
            if (count != null && count == 1L) {
                sync.expire(key, windowSeconds);
            }
            return Optional.ofNullable(count);
        } catch (Exception e) {
            LOG.warn("Valkey rate-limit increment failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception ignored) {
            }
        }
    }
}
