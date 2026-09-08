package com.jchess.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jchess.api.dto.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionManager {

    private static final Logger log = LoggerFactory.getLogger(GameSessionManager.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Set<SessionHolder>> gameSessions = new ConcurrentHashMap<>();
    private final Map<String, SessionHolder> sessionMap = new ConcurrentHashMap<>();

    public record SessionHolder(
            WebSocketSession session,
            Sinks.Many<String> sink,
            String gameId,
            String playerId
    ) {}

    public GameSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerSession(String gameId, String playerId, WebSocketSession session, Sinks.Many<String> sink) {
        SessionHolder holder = new SessionHolder(session, sink, gameId, playerId);
        sessionMap.put(session.getId(), holder);
        gameSessions.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(holder);
        log.info("Registered WebSocket session {} for game {} (player: {})", session.getId(), gameId, playerId);
    }

    public SessionHolder unregisterSession(WebSocketSession session) {
        SessionHolder holder = sessionMap.remove(session.getId());
        if (holder != null) {
            Set<SessionHolder> holders = gameSessions.get(holder.gameId());
            if (holders != null) {
                holders.remove(holder);
                if (holders.isEmpty()) {
                    gameSessions.remove(holder.gameId());
                }
            }
            log.info("Unregistered WebSocket session {} from game {}", session.getId(), holder.gameId());
        }
        return holder;
    }

    public void broadcast(String gameId, EventEnvelope<?> event) {
        Set<SessionHolder> holders = gameSessions.get(gameId);
        if (holders == null || holders.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            for (SessionHolder holder : holders) {
                if (holder.session().isOpen()) {
                    holder.sink().tryEmitNext(json);
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast event to game {}: {}", gameId, e.getMessage(), e);
        }
    }

    public void sendToSession(WebSocketSession session, EventEnvelope<?> event) {
        SessionHolder holder = sessionMap.get(session.getId());
        if (holder == null || !holder.session().isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            holder.sink().tryEmitNext(json);
        } catch (Exception e) {
            log.error("Failed to send event to session {}: {}", session.getId(), e.getMessage(), e);
        }
    }
}


