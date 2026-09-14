package com.jchess.service;

import com.jchess.ai.HybridChessAiService;
import com.jchess.api.dto.*;
import com.jchess.domain.engine.FenParser;
import com.jchess.domain.exception.*;
import com.jchess.domain.model.*;
import com.jchess.infrastructure.persistence.GameEntity;
import com.jchess.infrastructure.persistence.GameMoveEntity;
import com.jchess.infrastructure.persistence.GameMoveRepository;
import com.jchess.infrastructure.persistence.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final HybridChessAiService aiService;
    private final Clock clock;

    public GameService(GameRepository gameRepository, GameMoveRepository gameMoveRepository, HybridChessAiService aiService, Clock clock) {
        this.gameRepository = gameRepository;
        this.gameMoveRepository = gameMoveRepository;
        this.aiService = aiService;
        this.clock = clock;
    }

    @Transactional
    public CreateGameResponse createGame(CreateGameRequest request, String playerId, String playerName) {
        String gameId = "game-" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = clock.instant();

        TimeControlDto tc = request.timeControl();
        long initialTimeMs = tc.baseMinutes() * 60 * 1000L;
        GameMode mode = request.gameMode() != null ? request.gameMode() : GameMode.PVP;
        int aiLevel = request.aiLevel() != null ? request.aiLevel() : 600;

        GameEntity entity = new GameEntity();
        entity.setId(gameId);
        entity.setGameMode(mode);
        entity.setAiLevel(aiLevel);
        entity.setCurrentTurn(PieceColor.WHITE);
        entity.setCurrentFen(FenParser.INITIAL_FEN);
        entity.setBaseMinutes(tc.baseMinutes());
        entity.setIncrementSeconds(tc.incrementSeconds());
        entity.setWhiteRemainingTimeMs(initialTimeMs);
        entity.setBlackRemainingTimeMs(initialTimeMs);
        entity.setRemainingHints(3);
        int maxUndos;
        if (mode == GameMode.PVC) {
            if (aiLevel <= 750) maxUndos = 10;
            else if (aiLevel <= 1050) maxUndos = 8;
            else if (aiLevel <= 1450) maxUndos = 5;
            else maxUndos = 3;
        } else {
            maxUndos = 3;
        }
        entity.setMaxUndos(maxUndos);
        entity.setRemainingUndos(maxUndos);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        String preferredColor = request.preferredColor();
        boolean isBlack = "BLACK".equalsIgnoreCase(preferredColor);

        if (mode == GameMode.PVC) {
            entity.setStatus(GameStatus.ACTIVE);
            entity.setLastMoveAt(now);
            String aiDisplayName = getAiDisplayName(aiLevel);
            if (isBlack) {
                entity.setBlackPlayerId(playerId);
                entity.setBlackPlayerName(playerName);
                entity.setWhitePlayerId("ai-stockfish");
                entity.setWhitePlayerName(aiDisplayName);
            } else {
                entity.setWhitePlayerId(playerId);
                entity.setWhitePlayerName(playerName);
                entity.setBlackPlayerId("ai-stockfish");
                entity.setBlackPlayerName(aiDisplayName);
            }
        } else {
            entity.setStatus(GameStatus.WAITING_FOR_OPPONENT);
            if (isBlack) {
                entity.setBlackPlayerId(playerId);
                entity.setBlackPlayerName(playerName);
            } else {
                entity.setWhitePlayerId(playerId);
                entity.setWhitePlayerName(playerName);
            }
        }

        GameEntity saved = gameRepository.saveAndFlush(entity);
        log.info("[GAME_SERVICE] Game created: id={}, mode={}, aiLevel={}, white={}, black={}",
                saved.getId(), saved.getGameMode(), saved.getAiLevel(), saved.getWhitePlayerId(), saved.getBlackPlayerId());

        PlayerInfoDto white = saved.getWhitePlayerId() != null
                ? new PlayerInfoDto(saved.getWhitePlayerId(), saved.getWhitePlayerName(), saved.getWhiteRemainingTimeMs(), true)
                : null;
        PlayerInfoDto black = saved.getBlackPlayerId() != null
                ? new PlayerInfoDto(saved.getBlackPlayerId(), saved.getBlackPlayerName(), saved.getBlackRemainingTimeMs(), true)
                : null;

        return new CreateGameResponse(saved.getId(), saved.getGameMode(), saved.getAiLevel(), saved.getStatus(), white, black, saved.getVersion(), saved.getCreatedAt());
    }

    @Transactional
    public JoinGameResponse joinGame(String gameId, String playerId, String playerName) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    log.warn("[GAME_SERVICE] Join failed: Game not found id={}", gameId);
                    return new GameNotFoundException(gameId);
                });

        if (entity.getStatus() != GameStatus.WAITING_FOR_OPPONENT) {
            log.warn("[GAME_SERVICE] Join failed: Game {} is not in WAITING state (status={})", gameId, entity.getStatus());
            throw new ChessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "이미 대국이 시작되었거나 참가할 수 없는 상태입니다.", gameId, entity.getVersion());
        }

        Instant now = clock.instant();

        if (entity.getWhitePlayerId() == null) {
            if (playerId.equals(entity.getBlackPlayerId())) {
                log.warn("[GAME_SERVICE] Join failed: Player {} already joined as black in game {}", playerId, gameId);
                throw new ChessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "이미 대국에 참가한 플레이어입니다.", gameId, entity.getVersion());
            }
            entity.setWhitePlayerId(playerId);
            entity.setWhitePlayerName(playerName);
        } else if (entity.getBlackPlayerId() == null) {
            if (playerId.equals(entity.getWhitePlayerId())) {
                log.warn("[GAME_SERVICE] Join failed: Player {} already joined as white in game {}", playerId, gameId);
                throw new ChessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "이미 대국에 참가한 플레이어입니다.", gameId, entity.getVersion());
            }
            entity.setBlackPlayerId(playerId);
            entity.setBlackPlayerName(playerName);
        }

        entity.setStatus(GameStatus.ACTIVE);
        entity.setLastMoveAt(now);
        entity.setUpdatedAt(now);

        GameEntity saved = gameRepository.save(entity);
        log.info("[GAME_SERVICE] Player {} joined game {}. White={}, Black={}, Status={}",
                playerId, gameId, saved.getWhitePlayerId(), saved.getBlackPlayerId(), saved.getStatus());

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
                .orElseThrow(() -> {
                    log.warn("[GAME_SERVICE] PlayMove failed: Game not found id={}", gameId);
                    return new GameNotFoundException(gameId);
                });

        if (expectedVersion != null && !expectedVersion.equals(entity.getVersion())) {
            log.warn("[GAME_SERVICE] PlayMove failed: Version conflict for game {}. Expected: {}, Actual: {}",
                    gameId, expectedVersion, entity.getVersion());
            throw new GameVersionConflictException("대국 버전이 일치하지 않습니다. 최신 상태 동기화가 필요합니다.", gameId, entity.getVersion());
        }

        if (isEndedStatus(entity.getStatus())) {
            log.warn("[GAME_SERVICE] PlayMove failed: Game {} already finished with status {}", gameId, entity.getStatus());
            throw new GameAlreadyFinishedException("이미 종료된 대국입니다.", gameId, entity.getVersion());
        }

        boolean isWhite = playerId.equals(entity.getWhitePlayerId());
        boolean isBlack = playerId.equals(entity.getBlackPlayerId());

        if (!isWhite && !isBlack) {
            log.warn("[GAME_SERVICE] PlayMove failed: Unauthorized player {} in game {}", playerId, gameId);
            throw new UnauthorizedPlayerException("해당 대국의 참가자가 아닙니다.", gameId);
        }

        PieceColor playerColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;
        if (entity.getCurrentTurn() != playerColor) {
            log.warn("[GAME_SERVICE] PlayMove failed: Turn mismatch in game {}. Current turn: {}, Player: {}",
                    gameId, entity.getCurrentTurn(), playerColor);
            throw new NotYourTurnException("현재 플레이어의 차례가 아닙니다.", gameId, entity.getVersion());
        }

        Instant now = clock.instant();

        if (entity.getLastMoveAt() != null) {
            long elapsedMs = Duration.between(entity.getLastMoveAt(), now).toMillis();
            long remainingMs = isWhite ? entity.getWhiteRemainingTimeMs() : entity.getBlackRemainingTimeMs();
            if (remainingMs - elapsedMs <= 0) {
                log.info("[GAME_SERVICE] Timeout detected during playMove in game {} for color {}", gameId, playerColor);
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
            log.warn("[GAME_SERVICE] PlayMove failed: Illegal move {} -> {} (promotion={}) in game {}, fen='{}'",
                    request.from(), request.to(), request.promotion(), gameId, currentState.toFen());
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

        GameEntity saved = gameRepository.saveAndFlush(entity);
        log.debug("[GAME_SERVICE] Move executed successfully: gameId={}, move={}, newVersion={}, status={}",
                gameId, move.toUci(), saved.getVersion(), saved.getStatus());
        return toSnapshotResponse(saved);
    }

    @Transactional
    public GameSnapshotResponse resign(String gameId, String playerId) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    log.warn("[GAME_SERVICE] Resign failed: Game not found id={}", gameId);
                    return new GameNotFoundException(gameId);
                });

        if (isEndedStatus(entity.getStatus())) {
            log.warn("[GAME_SERVICE] Resign failed: Game {} already ended (status={})", gameId, entity.getStatus());
            throw new GameAlreadyFinishedException("이미 종료된 대국입니다.", gameId, entity.getVersion());
        }

        boolean isWhite = playerId.equals(entity.getWhitePlayerId());
        boolean isBlack = playerId.equals(entity.getBlackPlayerId());

        if (!isWhite && !isBlack) {
            log.warn("[GAME_SERVICE] Resign failed: Unauthorized player {} for game {}", playerId, gameId);
            throw new UnauthorizedPlayerException("해당 대국의 참가자가 아닙니다.", gameId);
        }

        PieceColor resignColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;
        entity.setStatus(GameStatus.RESIGNED);
        entity.setResult(resignColor == PieceColor.WHITE ? GameResult.BLACK_WON : GameResult.WHITE_WON);
        entity.setEndReason(GameEndReason.RESIGNATION);
        entity.setUpdatedAt(clock.instant());

        GameEntity saved = gameRepository.save(entity);
        log.info("[GAME_SERVICE] Game {} resigned by player {} (color={}), winner={}",
                gameId, playerId, resignColor, saved.getResult());
        return toSnapshotResponse(saved);
    }

    @Transactional
    public GameSnapshotResponse agreeDraw(String gameId, String playerId) {
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

        entity.setStatus(GameStatus.DRAW);
        entity.setResult(GameResult.DRAW);
        entity.setEndReason(GameEndReason.AGREED_DRAW);
        entity.setUpdatedAt(clock.instant());

        GameEntity saved = gameRepository.save(entity);
        log.info("[GAME_SERVICE] Game {} ended with AGREED_DRAW", gameId);
        return toSnapshotResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean evaluateAiDrawOffer(String gameId, String playerId) {
        GameEntity entity = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (entity.getGameMode() != GameMode.PVC || isEndedStatus(entity.getStatus())) {
            return false;
        }

        boolean isPlayerWhite = playerId.equals(entity.getWhitePlayerId());
        PieceColor aiColor = isPlayerWhite ? PieceColor.BLACK : PieceColor.WHITE;
        PieceColor playerColor = isPlayerWhite ? PieceColor.WHITE : PieceColor.BLACK;

        GameState state = GameState.fromFen(entity.getCurrentFen());
        int aiScore = calculateMaterialScore(state.board(), aiColor);
        int playerScore = calculateMaterialScore(state.board(), playerColor);
        int scoreDiff = aiScore - playerScore;

        int aiElo = entity.getAiLevel() != null ? entity.getAiLevel() : 600;

        log.info("[GAME_SERVICE] Evaluating AI Draw offer for game {}: aiScore={}, playerScore={}, scoreDiff={}, aiElo={}",
                gameId, aiScore, playerScore, scoreDiff, aiElo);

        if (scoreDiff <= -2) {
            return true;
        }
        if (scoreDiff >= 2) {
            return false;
        }
        return aiElo <= 1400;
    }

    @Transactional
    public HintResponse requestHint(String gameId, String playerId) {
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

        PieceColor playerColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;
        if (entity.getCurrentTurn() != playerColor) {
            throw new IllegalMoveException("본인의 착수 차례가 아닙니다.", gameId, entity.getVersion());
        }

        if (entity.getRemainingHints() <= 0) {
            throw new IllegalArgumentException("대국당 3회의 추천TIP 찬스를 모두 사용하셨습니다.");
        }

        entity.setRemainingHints(entity.getRemainingHints() - 1);
        entity.setUpdatedAt(clock.instant());
        gameRepository.save(entity);

        GameState state = GameState.fromFen(entity.getCurrentFen());
        Move bestMove = aiService.calculateBestMove(state, 2000).join();

        if (bestMove == null) {
            throw new IllegalStateException("추천 가능한 최선의 수를 계산하지 못했습니다.");
        }

        log.info("[GAME_SERVICE] Hint requested for game {}: move {} -> {}, remainingHints={}",
                gameId, bestMove.from().toAlgebraic(), bestMove.to().toAlgebraic(), entity.getRemainingHints());

        return new HintResponse(
                bestMove.from().toAlgebraic(),
                bestMove.to().toAlgebraic(),
                bestMove.promotion() != null ? bestMove.promotion().name() : null,
                entity.getRemainingHints(),
                "AI 추천 최선의 수입니다."
        );
    }

    private int calculateMaterialScore(Board board, PieceColor color) {
        int score = 0;
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = board.getPiece(Position.of(file, rank));
                if (piece != null && piece.color() == color) {
                    score += switch (piece.type()) {
                        case QUEEN -> 9;
                        case ROOK -> 5;
                        case BISHOP, KNIGHT -> 3;
                        case PAWN -> 1;
                        case KING -> 0;
                    };
                }
            }
        }
        return score;
    }

    @Transactional
    public UndoResponse undoMove(String gameId, String playerId) {
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

        if (entity.getRemainingUndos() <= 0) {
            throw new IllegalArgumentException("무르기 가능 횟수(" + entity.getMaxUndos() + "회)를 모두 소진하셨습니다.");
        }

        List<GameMoveEntity> moves = gameMoveRepository.findByGameIdOrderByIdAsc(gameId);
        if (moves.isEmpty()) {
            throw new IllegalStateException("되돌릴 수 있는 이전 착수 기록이 없습니다.");
        }

        int movesToRollback = 1;
        if (entity.getGameMode() == GameMode.PVC) {
            // PVC 모드: 플레이어 착수 차례로 되돌리기 위해 2수(상대 AI 수 + 내 수) 롤백 (단, 전체 수가 1수면 1수만 롤백)
            movesToRollback = Math.min(2, moves.size());
        }

        List<GameMoveEntity> movesToDelete = moves.subList(moves.size() - movesToRollback, moves.size());
        gameMoveRepository.deleteAll(movesToDelete);

        List<GameMoveEntity> remainingMoves = moves.subList(0, moves.size() - movesToRollback);
        String restoredFen;
        PieceColor restoredTurn;

        if (remainingMoves.isEmpty()) {
            restoredFen = FenParser.INITIAL_FEN;
            restoredTurn = PieceColor.WHITE;
        } else {
            GameMoveEntity lastRemaining = remainingMoves.get(remainingMoves.size() - 1);
            restoredFen = lastRemaining.getFenAfterMove();
            GameState state = GameState.fromFen(restoredFen);
            restoredTurn = state.activeColor();
        }

        entity.setCurrentFen(restoredFen);
        entity.setCurrentTurn(restoredTurn);
        entity.setStatus(GameStatus.ACTIVE);
        entity.setResult(null);
        entity.setEndReason(null);
        entity.setRemainingUndos(entity.getRemainingUndos() - 1);
        entity.setLastMoveAt(clock.instant());
        entity.setUpdatedAt(clock.instant());

        GameEntity saved = gameRepository.save(entity);
        log.info("[GAME_SERVICE] Undo executed for game {}: rolled back {} moves, remainingUndos={}/{}",
                gameId, movesToRollback, saved.getRemainingUndos(), saved.getMaxUndos());

        return new UndoResponse(
                true,
                saved.getRemainingUndos(),
                saved.getMaxUndos(),
                "무르기가 성공적으로 적용되었습니다.",
                toSnapshotResponse(saved)
        );
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

    private String getAiDisplayName(int aiLevel) {
        if (aiLevel <= 750) {
            return "Stockfish 8 (입문 · ELO 600)";
        } else if (aiLevel <= 1050) {
            return "Stockfish 11 (초급 · ELO 900)";
        } else if (aiLevel <= 1450) {
            return "Stockfish 14 (중급 · ELO 1300)";
        } else if (aiLevel <= 1850) {
            return "Stockfish 17 (고급 · ELO 1700)";
        } else {
            return "Stockfish 19 (마스터 · ELO 2000+)";
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

        Optional<GameMoveEntity> lastMoveOpt = gameMoveRepository.findTopByGameIdOrderByIdDesc(entity.getId());
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
                entity.getGameMode(),
                entity.getAiLevel(),
                entity.getRemainingHints(),
                entity.getMaxUndos(),
                entity.getRemainingUndos(),
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
