package com.jchess.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jchess.api.dto.*;
import com.jchess.domain.exception.ChessException;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceType;
import com.jchess.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Component
public class ChessWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChessWebSocketHandler.class);

    private final GameService gameService;
    private final GameSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public ChessWebSocketHandler(GameService gameService, GameSessionManager sessionManager, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String gameId = extractGameId(session.getHandshakeInfo().getUri());
        String playerId = extractPlayerId(session.getHandshakeInfo().getUri());

        reactor.core.publisher.Sinks.Many<String> sink = reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();
        sessionManager.registerSession(gameId, playerId, session, sink);

        reactor.core.publisher.Flux<String> outbound = reactor.core.publisher.Flux.defer(() -> {
            try {
                GameSnapshotResponse snapshot = gameService.getGameSnapshot(gameId);
                EventEnvelope<GameSnapshotResponse> snapshotEvent = EventEnvelope.of(
                        "evt-" + UUID.randomUUID().toString().substring(0, 8),
                        gameId,
                        snapshot.gameVersion(),
                        "GAME_STATE_SNAPSHOT",
                        snapshot
                );
                String json = objectMapper.writeValueAsString(snapshotEvent);
                sessionManager.broadcast(gameId, EventEnvelope.of(
                        "evt-" + UUID.randomUUID().toString().substring(0, 8),
                        gameId,
                        null,
                        "PLAYER_CONNECTION_CHANGED",
                        new PlayerConnectionChangedPayload(playerId, true)
                ));
                return sink.asFlux().startWith(json);
            } catch (Exception e) {
                log.error("Failed to generate initial snapshot: {}", e.getMessage());
                return sink.asFlux();
            }
        });

        Mono<Void> input = session.receive()
                .filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT)
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(payload -> handleIncomingMessage(session, gameId, playerId, payload))
                .doOnError(e -> log.error("WebSocket message processing error: {}", e.getMessage(), e))
                .then();

        Mono<Void> output = session.send(outbound.map(session::textMessage));

        return Mono.when(input, output)
                .doFinally(signalType -> {
                    sessionManager.unregisterSession(session);
                    sessionManager.broadcast(gameId, EventEnvelope.of(
                            "evt-" + UUID.randomUUID().toString().substring(0, 8),
                            gameId,
                            null,
                            "PLAYER_CONNECTION_CHANGED",
                            new PlayerConnectionChangedPayload(playerId, false)
                    ));
                });
    }

    private void handleIncomingMessage(WebSocketSession session, String gameId, String playerId, String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            String type = root.path("type").asText();
            String requestId = root.path("requestId").asText("req-" + UUID.randomUUID());
            Long expectedVersion = root.has("expectedGameVersion") ? root.path("expectedGameVersion").asLong() : null;
            JsonNode payload = root.path("payload");

            switch (type) {
                case "PLAY_MOVE" -> handlePlayMove(session, gameId, playerId, expectedVersion, requestId, payload);
                case "RESIGN" -> handleResign(gameId, playerId);
                case "SYNC_STATE" -> handleSyncState(session, gameId);
                case "OFFER_DRAW" -> handleOfferDraw(gameId, playerId);
                default -> log.warn("Unknown WebSocket command type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to parse WebSocket command: {}", e.getMessage(), e);
            sessionManager.sendToSession(session, EventEnvelope.of(
                    "evt-" + UUID.randomUUID().toString().substring(0, 8),
                    gameId,
                    null,
                    "ERROR",
                    new ErrorPayload("INVALID_REQUEST_PAYLOAD", "유효하지 않은 WebSocket 요청 형식입니다.", null, false)
            ));
        }
    }

    private void handlePlayMove(WebSocketSession session, String gameId, String playerId, Long expectedVersion, String requestId, JsonNode payload) {
        String from = payload.path("from").asText();
        String to = payload.path("to").asText();
        PieceType promotion = payload.hasNonNull("promotion")
                ? PieceType.valueOf(payload.path("promotion").asText().toUpperCase())
                : null;

        PlayMoveRequest request = new PlayMoveRequest(from, to, promotion);

        try {
            GameSnapshotResponse snapshot = gameService.playMove(gameId, request, playerId, expectedVersion, requestId);

            GameStateUpdatedPayload updatePayload = new GameStateUpdatedPayload(
                    0,
                    snapshot.turn(),
                    snapshot.lastMove(),
                    snapshot.fen(),
                    snapshot.isCheck(),
                    snapshot.whitePlayer() != null ? snapshot.whitePlayer().remainingTimeMs() : 0,
                    snapshot.blackPlayer() != null ? snapshot.blackPlayer().remainingTimeMs() : 0
            );

            EventEnvelope<GameStateUpdatedPayload> updateEvent = EventEnvelope.of(
                    "evt-" + UUID.randomUUID().toString().substring(0, 8),
                    gameId,
                    snapshot.gameVersion(),
                    "GAME_STATE_UPDATED",
                    updatePayload
            );

            sessionManager.broadcast(gameId, updateEvent);

            if (isGameOver(snapshot.gameStatus())) {
                EventEnvelope<GameEndedPayload> endEvent = EventEnvelope.of(
                        "evt-" + UUID.randomUUID().toString().substring(0, 8),
                        gameId,
                        snapshot.gameVersion(),
                        "GAME_ENDED",
                        new GameEndedPayload(snapshot.result(), snapshot.endReason(), snapshot.fen(), Instant.now())
                );
                sessionManager.broadcast(gameId, endEvent);
            }
        } catch (ChessException ex) {
            EventEnvelope<MoveRejectedPayload> rejectEvent = EventEnvelope.of(
                    "evt-" + UUID.randomUUID().toString().substring(0, 8),
                    gameId,
                    ex.getGameVersion(),
                    "MOVE_REJECTED",
                    new MoveRejectedPayload(requestId, ex.getErrorCode().name(), ex.getMessage(), ex.getGameVersion())
            );
            sessionManager.sendToSession(session, rejectEvent);
        } catch (Exception ex) {
            log.error("Failed to play move via WebSocket: {}", ex.getMessage(), ex);
        }
    }

    private void handleResign(String gameId, String playerId) {
        try {
            GameSnapshotResponse snapshot = gameService.resign(gameId, playerId);
            EventEnvelope<GameEndedPayload> endEvent = EventEnvelope.of(
                    "evt-" + UUID.randomUUID().toString().substring(0, 8),
                    gameId,
                    snapshot.gameVersion(),
                    "GAME_ENDED",
                    new GameEndedPayload(snapshot.result(), snapshot.endReason(), snapshot.fen(), Instant.now())
            );
            sessionManager.broadcast(gameId, endEvent);
        } catch (Exception e) {
            log.error("Failed to resign via WebSocket: {}", e.getMessage(), e);
        }
    }

    private void handleSyncState(WebSocketSession session, String gameId) {
        try {
            GameSnapshotResponse snapshot = gameService.getGameSnapshot(gameId);
            EventEnvelope<GameSnapshotResponse> snapshotEvent = EventEnvelope.of(
                    "evt-" + UUID.randomUUID().toString().substring(0, 8),
                    gameId,
                    snapshot.gameVersion(),
                    "GAME_STATE_SNAPSHOT",
                    snapshot
            );
            sessionManager.sendToSession(session, snapshotEvent);
        } catch (Exception e) {
            log.error("Failed to sync state via WebSocket: {}", e.getMessage(), e);
        }
    }

    private void handleOfferDraw(String gameId, String playerId) {
        EventEnvelope<DrawOfferPayload> offerEvent = EventEnvelope.of(
                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                gameId,
                null,
                "DRAW_OFFERED",
                new DrawOfferPayload(playerId)
        );
        sessionManager.broadcast(gameId, offerEvent);
    }

    private boolean isGameOver(GameStatus status) {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE
                || status == GameStatus.DRAW || status == GameStatus.RESIGNED || status == GameStatus.TIMEOUT;
    }

    private String extractGameId(URI uri) {
        String path = uri.getPath();
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("games".equals(segments[i])) {
                return segments[i + 1];
            }
        }
        return segments[segments.length - 1];
    }

    private String extractPlayerId(URI uri) {
        String query = uri.getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2 && ("playerId".equals(kv[0]) || "token".equals(kv[0]))) {
                    return kv[1];
                }
            }
        }
        return "anonymous-" + UUID.randomUUID().toString().substring(0, 6);
    }

}
