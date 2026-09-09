package com.jchess.api.controller;

import com.jchess.ai.AsyncAiMoveExecutor;
import com.jchess.api.dto.*;
import com.jchess.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;
    private final AsyncAiMoveExecutor aiMoveExecutor;

    public GameController(GameService gameService, AsyncAiMoveExecutor aiMoveExecutor) {
        this.gameService = gameService;
        this.aiMoveExecutor = aiMoveExecutor;
    }

    @PostMapping
    public ResponseEntity<CreateGameResponse> createGame(
            @RequestBody(required = false) CreateGameRequest request,
            @RequestHeader(value = "X-Player-Id", defaultValue = "anonymous-player") String playerId,
            @RequestHeader(value = "X-Player-Name", defaultValue = "Anonymous") String playerName
    ) {
        if (request == null) {
            request = new CreateGameRequest(TimeControlDto.standard(), "WHITE");
        }
        CreateGameResponse response = gameService.createGame(request, playerId, playerName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<JoinGameResponse> joinGame(
            @PathVariable String gameId,
            @RequestHeader(value = "X-Player-Id", defaultValue = "guest-player") String playerId,
            @RequestHeader(value = "X-Player-Name", defaultValue = "Guest") String playerName
    ) {
        JoinGameResponse response = gameService.joinGame(gameId, playerId, playerName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameSnapshotResponse> getGameSnapshot(@PathVariable String gameId) {
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
        GameSnapshotResponse response = gameService.playMove(gameId, request, playerId, expectedVersion, requestId);
        aiMoveExecutor.triggerAiMoveIfApplicable(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/resign")
    public ResponseEntity<GameSnapshotResponse> resign(
            @PathVariable String gameId,
            @RequestHeader("X-Player-Id") String playerId
    ) {
        GameSnapshotResponse response = gameService.resign(gameId, playerId);
        return ResponseEntity.ok(response);
    }
}

