package com.jchess.ai;

import com.jchess.api.dto.*;
import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameState;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;
import com.jchess.service.GameService;
import com.jchess.websocket.GameSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

@Component
public class AsyncAiMoveExecutor {

    private static final Logger log = LoggerFactory.getLogger(AsyncAiMoveExecutor.class);
    private static final String AI_ID = "ai-stockfish";

    private final GameService gameService;
    private final HybridChessAiService aiService;
    private final GameSessionManager sessionManager;
    private final Executor aiTaskExecutor;

    public AsyncAiMoveExecutor(GameService gameService,
                               HybridChessAiService aiService,
                               GameSessionManager sessionManager,
                               @Qualifier("aiEngineTaskExecutor") Executor aiTaskExecutor) {
        this.gameService = gameService;
        this.aiService = aiService;
        this.sessionManager = sessionManager;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    public void triggerAiMoveIfApplicable(GameSnapshotResponse snapshot) {
        if (snapshot.gameMode() != GameMode.PVC || isGameOver(snapshot.gameStatus())) {
            return;
        }

        boolean isAiTurn = (snapshot.turn() == PieceColor.WHITE && snapshot.whitePlayer() != null && AI_ID.equals(snapshot.whitePlayer().id()))
                || (snapshot.turn() == PieceColor.BLACK && snapshot.blackPlayer() != null && AI_ID.equals(snapshot.blackPlayer().id()));

        if (!isAiTurn) return;

        aiTaskExecutor.execute(() -> {
            try {
                String gameId = snapshot.gameId();
                int aiLevel = snapshot.aiLevel() != null ? snapshot.aiLevel() : 2000;
                GameState gameState = GameState.fromFen(snapshot.fen());

                log.info("[AI_EXEC] Starting AI move calculation for game {}, aiLevel={}, fen='{}'", gameId, aiLevel, snapshot.fen());

                aiService.calculateBestMove(gameState, aiLevel).thenAccept(bestMove -> {
                    if (bestMove == null) {
                        log.warn("[AI_EXEC] AI returned null move for game {}", gameId);
                        return;
                    }
                    try {
                        log.info("[AI_EXEC] AI selected move {} -> {} (promotion={}) for game {}",
                                bestMove.from().toAlgebraic(), bestMove.to().toAlgebraic(), bestMove.promotion(), gameId);

                        PlayMoveRequest req = new PlayMoveRequest(
                                bestMove.from().toAlgebraic(),
                                bestMove.to().toAlgebraic(),
                                bestMove.promotion()
                        );
                        String reqId = "ai-req-" + UUID.randomUUID().toString().substring(0, 8);
                        GameSnapshotResponse updated = gameService.playMove(gameId, req, AI_ID, null, reqId);

                        GameStateUpdatedPayload payload = new GameStateUpdatedPayload(
                                0, updated.turn(), updated.lastMove(), updated.fen(), updated.isCheck(),
                                updated.whitePlayer() != null ? updated.whitePlayer().remainingTimeMs() : 0,
                                updated.blackPlayer() != null ? updated.blackPlayer().remainingTimeMs() : 0
                        );
                        sessionManager.broadcast(gameId, EventEnvelope.of(
                                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                                gameId, updated.gameVersion(), "GAME_STATE_UPDATED", payload
                        ));

                        log.info("[AI_EXEC] AI move broadcast completed: gameId={}, newVersion={}, nextTurn={}",
                                gameId, updated.gameVersion(), updated.turn());

                        if (isGameOver(updated.gameStatus())) {
                            sessionManager.broadcast(gameId, EventEnvelope.of(
                                    "evt-" + UUID.randomUUID().toString().substring(0, 8),
                                    gameId, updated.gameVersion(), "GAME_ENDED",
                                    new GameEndedPayload(updated.result(), updated.endReason(), updated.fen(), Instant.now())
                            ));
                            log.info("[AI_EXEC] Game ended after AI move: gameId={}, status={}, result={}",
                                    gameId, updated.gameStatus(), updated.result());
                        }
                    } catch (Exception ex) {
                        log.error("[AI_EXEC_ERROR] Error applying AI move in {}: {}", gameId, ex.getMessage(), ex);
                    }
                });
            } catch (Exception e) {
                log.error("[AI_EXEC_ERROR] AI trigger error for {}: {}", snapshot.gameId(), e.getMessage(), e);
            }
        });
    }

    private boolean isGameOver(GameStatus s) {
        return s == GameStatus.CHECKMATE || s == GameStatus.STALEMATE || s == GameStatus.DRAW
                || s == GameStatus.RESIGNED || s == GameStatus.TIMEOUT;
    }
}
