package com.jchess.api.controller;

import com.jchess.ai.AsyncAiMoveExecutor;
import com.jchess.api.dto.*;
import com.jchess.service.GameService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService gameService;
    private final AsyncAiMoveExecutor aiMoveExecutor;

    public GameController(GameService gameService, AsyncAiMoveExecutor aiMoveExecutor) {
        this.gameService = gameService;
        this.aiMoveExecutor = aiMoveExecutor;
    }

    private String decodePlayerName(String name) {
        if (name == null || name.isBlank()) return "Anonymous";
        try {
            return URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return name;
        }
    }

    @PostMapping
    public ResponseEntity<CreateGameResponse> createGame(
            @RequestBody(required = false) CreateGameRequest request,
            @RequestHeader(value = "X-Player-Id", defaultValue = "anonymous-player") String playerId,
            @RequestHeader(value = "X-Player-Name", defaultValue = "Anonymous") String rawPlayerName
    ) {
        String playerName = decodePlayerName(rawPlayerName);
        log.info("[REST_API] POST /api/v1/games - playerId: {}, playerName: {}, mode: {}",
                playerId, playerName, request != null ? request.gameMode() : "PVP");
        if (request == null) {
            request = new CreateGameRequest(TimeControlDto.standard(), "WHITE");
        }
        CreateGameResponse response = gameService.createGame(request, playerId, playerName);
        log.info("[REST_API] Game created successfully: gameId={}, mode={}, status={}",
                response.gameId(), response.gameMode(), response.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<JoinGameResponse> joinGame(
            @PathVariable String gameId,
            @RequestHeader(value = "X-Player-Id", defaultValue = "guest-player") String playerId,
            @RequestHeader(value = "X-Player-Name", defaultValue = "Guest") String rawPlayerName
    ) {
        String playerName = decodePlayerName(rawPlayerName);
        log.info("[REST_API] POST /api/v1/games/{}/join - playerId: {}, playerName: {}", gameId, playerId, playerName);
        JoinGameResponse response = gameService.joinGame(gameId, playerId, playerName);
        log.info("[REST_API] Player joined successfully: gameId={}, playerId={}, status={}, version={}",
                gameId, playerId, response.status(), response.gameVersion());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameSnapshotResponse> getGameSnapshot(@PathVariable String gameId) {
        log.debug("[REST_API] GET /api/v1/games/{}", gameId);
        GameSnapshotResponse response = gameService.getGameSnapshot(gameId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/moves")
    public ResponseEntity<GameSnapshotResponse> playMove(
            @PathVariable String gameId,
            @Valid @RequestBody PlayMoveRequest request,
            @RequestHeader("X-Player-Id") String playerId,
            @RequestHeader(value = "X-Expected-Version", required = false) Long expectedVersion,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        log.info("[REST_API] POST /api/v1/games/{}/moves - playerId: {}, from: {}, to: {}, promotion: {}, expectedVersion: {}, reqId: {}",
                gameId, playerId, request.from(), request.to(), request.promotion(), expectedVersion, requestId);
        GameSnapshotResponse response = gameService.playMove(gameId, request, playerId, expectedVersion, requestId);
        log.info("[REST_API] Move confirmed: gameId={}, nextTurn={}, version={}, fen='{}'",
                gameId, response.turn(), response.gameVersion(), response.fen());
        aiMoveExecutor.triggerAiMoveIfApplicable(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/resign")
    public ResponseEntity<GameSnapshotResponse> resign(
            @PathVariable String gameId,
            @RequestHeader("X-Player-Id") String playerId
    ) {
        log.info("[REST_API] POST /api/v1/games/{}/resign - playerId: {}", gameId, playerId);
        GameSnapshotResponse response = gameService.resign(gameId, playerId);
        log.info("[REST_API] Game resigned: gameId={}, result={}, reason={}", gameId, response.result(), response.endReason());
        return ResponseEntity.ok(response);
    }
}



