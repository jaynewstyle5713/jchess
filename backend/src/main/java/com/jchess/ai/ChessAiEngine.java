package com.jchess.ai;

import com.jchess.domain.model.GameState;
import com.jchess.domain.model.Move;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface ChessAiEngine {
    
    CompletableFuture<Move> findBestMove(GameState gameState, int targetElo, Duration timeout);

    boolean isAvailable();
}
