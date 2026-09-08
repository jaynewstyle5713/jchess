package com.jchess.service;

import com.jchess.api.dto.*;
import com.jchess.domain.engine.FenParser;
import com.jchess.domain.exception.*;
import com.jchess.domain.model.*;
import com.jchess.infrastructure.persistence.GameEntity;
import com.jchess.infrastructure.persistence.GameMoveEntity;
import com.jchess.infrastructure.persistence.GameMoveRepository;
import com.jchess.infrastructure.persistence.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final Clock clock;

    public GameService(GameRepository gameRepository, GameMoveRepository gameMoveRepository, Clock clock) {
        this.gameRepository = gameRepository;
        this.gameMoveRepository = gameMoveRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateGameResponse createGame(CreateGameRequest request, String playerId, String playerName) {
        String gameId = "game-" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = clock.instant();

        TimeControlDto tc = request.timeControl();
        long initialTimeMs = tc.baseMinutes() * 60 * 1000L;

        GameEntity entity = new GameEntity();
        entity.setId(gameId);
        entity.setStatus(GameStatus.WAITING_FOR_OPPONENT);
        entity.setCurrentTurn(PieceColor.WHITE);
        entity.setCurrentFen(FenParser.INITIAL_FEN);
        entity.setBaseMinutes(tc.baseMinutes());
        entity.setIncrementSeconds(tc.incrementSeconds());
        entity.setWhiteRemainingTimeMs(initialTimeMs);
        entity.setBlackRemainingTimeMs(initialTimeMs);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        String preferredColor = request.preferredColor();
        if ("BLACK".equalsIgnoreCase(preferredColor)) {
            entity.setBlackPlayerId(playerId);
            entity.setBlackPlayerName(playerName);
        } else {
            entity.setWhitePlayerId(playerId);
            entity.setWhitePlayerName(playerName);
        }

        GameEntity saved = gameRepository.save(entity);

        PlayerInfoDto white = saved.getWhitePlayerId() != null
                ? new PlayerInfoDto(saved.getWhitePlayerId(), saved.getWhitePlayerName(), saved.getWhiteRemainingTimeMs(), true)
                : null;
        PlayerInfoDto black = saved.getBlackPlayerId() != null
                ? new PlayerInfoDto(saved.getBlackPlayerId(), saved.getBlackPlayerName(), saved.getBlackRemainingTimeMs(), true)
                : null;

        return new CreateGameResponse(saved.getId(), saved.getStatus(), white, black, saved.getVersion(), saved.getCreatedAt());
    }

    @Transactional
    public JoinGameResponse joinGame(String gameId, String playerId, String playerName) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (entity.getStatus() != GameStatus.WAITING_FOR_OPPONENT) {
            throw new ChessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "이미 대국이 시작되었거나 참가할 수 없는 상태입니다.", gameId, entity.getVersion());
        }

        Instant now = clock.instant();

        if (entity.getWhitePlayerId() == null) {
            if (playerId.equals(entity.getBlackPlayerId())) {
                throw new ChessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "이미 대국에 참가한 플레이어입니다.", gameId, entity.getVersion());
            }
            entity.setWhitePlayerId(playerId);
            entity.setWhitePlayerName(playerName);
        } else if (entity.getBlackPlayerId() == null) {
            if (playerId.equals(entity.getWhitePlayerId())) {
                throw new ChessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "이미 대국에 참가한 플레이어입니다.", gameId, entity.getVersion());
            }
            entity.setBlackPlayerId(playerId);
            entity.setBlackPlayerName(playerName);
        }

        entity.setStatus(GameStatus.ACTIVE);
        entity.setLastMoveAt(now);
        entity.setUpdatedAt(now);

        GameEntity saved = gameRepository.save(entity);

        PlayerInfoDto white = new PlayerInfoDto(saved.getWhitePlayerId(), saved.getWhitePlayerName(), saved.getWhiteRemainingTimeMs(), true);
        PlayerInfoDto black = new PlayerInfoDto(saved.getBlackPlayerId(), saved.getBlackPlayerName(), saved.getBlackRemainingTimeMs(), true);

        return new JoinGameResponse(saved.getId(), saved.getStatus(), white, black, saved.getVersion());
    }

    @Transactional
    public GameSnapshotResponse getGameSnapshot(String gameId) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        checkAndApplyTimeoutIfNecessary(entity);
        return toSnapshotResponse(entity);
    }

    @Transactional
    public GameSnapshotResponse playMove(String gameId, PlayMoveRequest request, String playerId, Long expectedVersion, String requestId) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (expectedVersion != null && !expectedVersion.equals(entity.getVersion())) {
            throw new GameVersionConflictException("대국 버전이 일치하지 않습니다. 최신 상태 동기화가 필요합니다.", gameId, entity.getVersion());
        }

        if (isEndedStatus(entity.getStatus())) {
            throw new GameAlreadyFinishedException("이미 종료된 대국입니다.", gameId, entity.getVersion());
        }

        boolean isWhite = playerId.equals(entity.getWhitePlayerId());
        boolean isBlack = playerId.equals(entity.getBlackPlayerId());

        if (!isWhite && !isBlack) {
            throw new UnauthorizedPlayerException("해당 대국의 참가자가 아닙니다.", gameId);
        }

        PieceColor playerColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;
        if (entity.getCurrentTurn() != playerColor) {
            throw new NotYourTurnException("현재 플레이어의 차례가 아닙니다.", gameId, entity.getVersion());
        }

        Instant now = clock.instant();

        if (entity.getLastMoveAt() != null) {
            long elapsedMs = Duration.between(entity.getLastMoveAt(), now).toMillis();
            long remainingMs = isWhite ? entity.getWhiteRemainingTimeMs() : entity.getBlackRemainingTimeMs();
            if (remainingMs - elapsedMs <= 0) {
                applyTimeout(entity, playerColor);
                gameRepository.save(entity);
                return toSnapshotResponse(entity);
            }
            long updatedRemainingMs = remainingMs - elapsedMs + (entity.getIncrementSeconds() * 1000L);
            if (isWhite) {
                entity.setWhiteRemainingTimeMs(updatedRemainingMs);
            } else {
                entity.setBlackRemainingTimeMs(updatedRemainingMs);
            }
        }
        entity.setLastMoveAt(now);

        GameState currentState = GameState.fromFen(entity.getCurrentFen());
        Position from = Position.fromAlgebraic(request.from());
        Position to = Position.fromAlgebraic(request.to());
        Move move = Move.of(from, to, request.promotion());

        if (!currentState.isLegalMove(move)) {
            throw new IllegalMoveException("합법적인 수가 아닙니다: " + request.from() + " -> " + request.to(), gameId, entity.getVersion());
        }

        GameState nextState = currentState.applyMove(move);

        GameMoveEntity moveEntity = new GameMoveEntity();
        moveEntity.setGameId(gameId);
        moveEntity.setMoveNumber(currentState.fullmoveNumber());
        moveEntity.setPlayerColor(playerColor.name());
        moveEntity.setFromSquare(request.from());
        moveEntity.setToSquare(request.to());
        moveEntity.setPromotionPiece(request.promotion() != null ? request.promotion().name() : null);
        moveEntity.setMoveNotation(move.toUci());
        moveEntity.setFenAfterMove(nextState.toFen());
        moveEntity.setPlayedAt(now);
        gameMoveRepository.save(moveEntity);

        entity.setCurrentFen(nextState.toFen());
        entity.setCurrentTurn(nextState.activeColor());
        entity.setStatus(nextState.status());
        entity.setResult(nextState.result());
        entity.setEndReason(nextState.endReason());
        entity.setUpdatedAt(now);

        GameEntity saved = gameRepository.save(entity);
        return toSnapshotResponse(saved);
    }

    @Transactional
    public GameSnapshotResponse resign(String gameId, String playerId) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (isEndedStatus(entity.getStatus())) {
            throw new GameAlreadyFinishedException("이미 종료된 대국입니다.", gameId, entity.getVersion());
        }

        boolean isWhite = playerId.equals(entity.getWhitePlayerId());
        boolean isBlack = playerId.equals(entity.getBlackPlayerId());

        if (!isWhite && !isBlack) {
            throw new UnauthorizedPlayerException("해당 대국의 참가자가 아닙니다.", gameId);
        }

        PieceColor resignColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;
        entity.setStatus(GameStatus.RESIGNED);
        entity.setResult(resignColor == PieceColor.WHITE ? GameResult.BLACK_WON : GameResult.WHITE_WON);
        entity.setEndReason(GameEndReason.RESIGNATION);
        entity.setUpdatedAt(clock.instant());

        GameEntity saved = gameRepository.save(entity);
        return toSnapshotResponse(saved);
    }


    private void checkAndApplyTimeoutIfNecessary(GameEntity entity) {
        if ((entity.getStatus() == GameStatus.ACTIVE || entity.getStatus() == GameStatus.CHECK) && entity.getLastMoveAt() != null) {
            Instant now = clock.instant();
            long elapsedMs = Duration.between(entity.getLastMoveAt(), now).toMillis();
            boolean isWhiteTurn = entity.getCurrentTurn() == PieceColor.WHITE;
            long remainingMs = isWhiteTurn ? entity.getWhiteRemainingTimeMs() : entity.getBlackRemainingTimeMs();

            if (remainingMs - elapsedMs <= 0) {
                applyTimeout(entity, entity.getCurrentTurn());
                gameRepository.save(entity);
            }
        }
    }

    private void applyTimeout(GameEntity entity, PieceColor timedOutColor) {
        entity.setStatus(GameStatus.TIMEOUT);
        entity.setResult(timedOutColor == PieceColor.WHITE ? GameResult.BLACK_WON : GameResult.WHITE_WON);
        entity.setEndReason(GameEndReason.TIMEOUT);
        entity.setUpdatedAt(clock.instant());
        if (timedOutColor == PieceColor.WHITE) {
            entity.setWhiteRemainingTimeMs(0);
        } else {
            entity.setBlackRemainingTimeMs(0);
        }
    }

    private boolean isEndedStatus(GameStatus status) {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE
                || status == GameStatus.DRAW || status == GameStatus.RESIGNED || status == GameStatus.TIMEOUT;
    }

    private GameSnapshotResponse toSnapshotResponse(GameEntity entity) {
        PlayerInfoDto white = entity.getWhitePlayerId() != null
                ? new PlayerInfoDto(entity.getWhitePlayerId(), entity.getWhitePlayerName(), entity.getWhiteRemainingTimeMs(), true)
                : null;
        PlayerInfoDto black = entity.getBlackPlayerId() != null
                ? new PlayerInfoDto(entity.getBlackPlayerId(), entity.getBlackPlayerName(), entity.getBlackRemainingTimeMs(), true)
                : null;

        Optional<GameMoveEntity> lastMoveOpt = gameMoveRepository.findTopByGameIdOrderByMoveNumberDesc(entity.getId());
        MoveDto lastMove = lastMoveOpt.map(m -> new MoveDto(
                m.getFromSquare(),
                m.getToSquare(),
                null,
                null,
                m.getPromotionPiece(),
                m.getMoveNotation()
        )).orElse(null);

        boolean isCheck = GameState.fromFen(entity.getCurrentFen()).isInCheck();

        return new GameSnapshotResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCurrentTurn(),
                entity.getCurrentFen(),
                white,
                black,
                lastMove,
                isCheck,
                entity.getResult(),
                entity.getEndReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
