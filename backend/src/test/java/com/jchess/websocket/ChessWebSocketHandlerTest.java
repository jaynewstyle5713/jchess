package com.jchess.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jchess.ai.AsyncAiMoveExecutor;
import com.jchess.api.dto.*;
import com.jchess.config.ClockConfig;
import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;
import com.jchess.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ChessWebSocketHandlerTest {

    private GameService gameService;
    private GameSessionManager sessionManager;
    private ObjectMapper objectMapper;
    private AsyncAiMoveExecutor aiMoveExecutor;
    private ChessWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        gameService = Mockito.mock(GameService.class);
        objectMapper = new ClockConfig().objectMapper();
        sessionManager = new GameSessionManager(objectMapper);
        aiMoveExecutor = Mockito.mock(AsyncAiMoveExecutor.class);
        handler = new ChessWebSocketHandler(gameService, sessionManager, objectMapper, aiMoveExecutor);
    }

    @Test
    @DisplayName("GameSessionManager 세션 등록, 브로드캐스트 및 세션 전송 검증")
    void sessionManagerBroadcastAndSend() throws Exception {
        WebSocketSession session1 = Mockito.mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("sess-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.textMessage(any())).thenAnswer(inv -> {
            WebSocketMessage msg = Mockito.mock(WebSocketMessage.class);
            when(msg.getPayloadAsText()).thenReturn(inv.getArgument(0));
            return msg;
        });

        Sinks.Many<String> sink1 = Sinks.many().replay().all();
        sessionManager.registerSession("game-1", "user-1", session1, sink1);

        List<String> received = new ArrayList<>();
        sink1.asFlux().subscribe(received::add);

        EventEnvelope<String> event = EventEnvelope.of("evt-1", "game-1", 1L, "TEST_EVENT", "hello");
        sessionManager.broadcast("game-1", event);

        assertThat(received).hasSize(1);
        assertThat(received.get(0)).contains("TEST_EVENT");
        assertThat(received.get(0)).contains("hello");

        sessionManager.unregisterSession(session1);
    }

    @Test
    @DisplayName("PLAY_MOVE 메시지 수신 시 GameService 호출 및 GAME_STATE_UPDATED 브로드캐스트 검증")
    void handlePlayMoveMessage() throws Exception {
        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);
        when(session.textMessage(any())).thenAnswer(inv -> {
            WebSocketMessage msg = Mockito.mock(WebSocketMessage.class);
            when(msg.getPayloadAsText()).thenReturn(inv.getArgument(0));
            return msg;
        });

        HandshakeInfo handshake = Mockito.mock(HandshakeInfo.class);
        when(handshake.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/games/game-100?playerId=user-white"));
        when(session.getHandshakeInfo()).thenReturn(handshake);

        GameSnapshotResponse initialSnapshot = new GameSnapshotResponse(
                "game-100", GameMode.PVP, 2000, GameStatus.ACTIVE, 1L, PieceColor.WHITE, "initial-fen",
                new PlayerInfoDto("user-white", "Alice", 600000L, true),
                new PlayerInfoDto("user-black", "Bob", 600000L, true),
                null, false, null, null, Instant.now(), Instant.now()
        );
        when(gameService.getGameSnapshot("game-100")).thenReturn(initialSnapshot);

        GameSnapshotResponse updatedSnapshot = new GameSnapshotResponse(
                "game-100", GameMode.PVP, 2000, GameStatus.ACTIVE, 2L, PieceColor.BLACK, "fen-after-e4",
                new PlayerInfoDto("user-white", "Alice", 595000L, true),
                new PlayerInfoDto("user-black", "Bob", 600000L, true),
                new MoveDto("e2", "e4", null, null, null, "e2e4"),
                false, null, null, Instant.now(), Instant.now()
        );
        when(gameService.playMove(eq("game-100"), any(PlayMoveRequest.class), eq("user-white"), eq(1L), eq("req-1")))
                .thenReturn(updatedSnapshot);

        String playMoveJson = """
                {
                  "type": "PLAY_MOVE",
                  "requestId": "req-1",
                  "gameId": "game-100",
                  "expectedGameVersion": 1,
                  "payload": {
                    "from": "e2",
                    "to": "e4",
                    "promotion": null
                  }
                }
                """;

        WebSocketMessage inboundMsg = Mockito.mock(WebSocketMessage.class);
        when(inboundMsg.getType()).thenReturn(WebSocketMessage.Type.TEXT);
        when(inboundMsg.getPayloadAsText()).thenReturn(playMoveJson);
        when(session.receive()).thenReturn(Flux.just(inboundMsg));
        when(session.send(any())).thenReturn(reactor.core.publisher.Mono.empty());

        handler.handle(session).block();

        Mockito.verify(gameService).playMove(eq("game-100"), any(PlayMoveRequest.class), eq("user-white"), eq(1L), eq("req-1"));
    }

    @Test
    @DisplayName("RESIGN 메시지 수신 시 GameService.resign 호출 및 GAME_ENDED 브로드캐스트 검증")
    void handleResignMessage() throws Exception {
        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-2");
        when(session.isOpen()).thenReturn(true);
        when(session.textMessage(any())).thenAnswer(inv -> {
            WebSocketMessage msg = Mockito.mock(WebSocketMessage.class);
            when(msg.getPayloadAsText()).thenReturn(inv.getArgument(0));
            return msg;
        });

        HandshakeInfo handshake = Mockito.mock(HandshakeInfo.class);
        when(handshake.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/games/game-200?playerId=user-white"));
        when(session.getHandshakeInfo()).thenReturn(handshake);

        GameSnapshotResponse resignSnapshot = new GameSnapshotResponse(
                "game-200", GameMode.PVP, 2000, GameStatus.RESIGNED, 2L, PieceColor.WHITE, "fen-after-resign",
                new PlayerInfoDto("user-white", "Alice", 500000L, true),
                new PlayerInfoDto("user-black", "Bob", 600000L, true),
                null, false, com.jchess.domain.model.GameResult.BLACK_WON, com.jchess.domain.model.GameEndReason.RESIGNATION,
                Instant.now(), Instant.now()
        );
        when(gameService.getGameSnapshot("game-200")).thenReturn(resignSnapshot);
        when(gameService.resign("game-200", "user-white")).thenReturn(resignSnapshot);

        String resignJson = """
                {
                  "type": "RESIGN",
                  "requestId": "req-resign",
                  "gameId": "game-200",
                  "expectedGameVersion": 1,
                  "payload": {}
                }
                """;

        WebSocketMessage inboundMsg = Mockito.mock(WebSocketMessage.class);
        when(inboundMsg.getType()).thenReturn(WebSocketMessage.Type.TEXT);
        when(inboundMsg.getPayloadAsText()).thenReturn(resignJson);
        when(session.receive()).thenReturn(Flux.just(inboundMsg));
        when(session.send(any())).thenReturn(reactor.core.publisher.Mono.empty());

        handler.handle(session).block();

        Mockito.verify(gameService).resign("game-200", "user-white");
    }

    @Test
    @DisplayName("SYNC_STATE 메시지 수신 시 GameService.getGameSnapshot 호출 및 SNAPSHOT 회신 검증")
    void handleSyncStateMessage() throws Exception {
        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-3");
        when(session.isOpen()).thenReturn(true);
        when(session.textMessage(any())).thenAnswer(inv -> {
            WebSocketMessage msg = Mockito.mock(WebSocketMessage.class);
            when(msg.getPayloadAsText()).thenReturn(inv.getArgument(0));
            return msg;
        });

        HandshakeInfo handshake = Mockito.mock(HandshakeInfo.class);
        when(handshake.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/games/game-300?playerId=user-black"));
        when(session.getHandshakeInfo()).thenReturn(handshake);

        GameSnapshotResponse snapshot = new GameSnapshotResponse(
                "game-300", GameMode.PVP, 2000, GameStatus.ACTIVE, 3L, PieceColor.WHITE, "sync-fen",
                new PlayerInfoDto("user-white", "Alice", 500000L, true),
                new PlayerInfoDto("user-black", "Bob", 600000L, true),
                null, false, null, null, Instant.now(), Instant.now()
        );
        when(gameService.getGameSnapshot("game-300")).thenReturn(snapshot);

        String syncJson = """
                {
                  "type": "SYNC_STATE",
                  "requestId": "req-sync",
                  "gameId": "game-300",
                  "expectedGameVersion": 3,
                  "payload": {}
                }
                """;

        WebSocketMessage inboundMsg = Mockito.mock(WebSocketMessage.class);
        when(inboundMsg.getType()).thenReturn(WebSocketMessage.Type.TEXT);
        when(inboundMsg.getPayloadAsText()).thenReturn(syncJson);
        when(session.receive()).thenReturn(Flux.just(inboundMsg));
        when(session.send(any())).thenReturn(reactor.core.publisher.Mono.empty());

        handler.handle(session).block();

        Mockito.verify(gameService, Mockito.atLeastOnce()).getGameSnapshot("game-300");
    }
}
