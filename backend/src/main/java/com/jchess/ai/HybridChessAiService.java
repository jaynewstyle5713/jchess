package com.jchess.ai;

import com.jchess.domain.model.GameState;
import com.jchess.domain.model.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class HybridChessAiService {

    private static final Logger log = LoggerFactory.getLogger(HybridChessAiService.class);

    private final StockfishUciEngineAdapter stockfishAdapter;
    private final JavaFallbackChessAiEngine fallbackEngine;

    public HybridChessAiService(StockfishUciEngineAdapter stockfishAdapter, JavaFallbackChessAiEngine fallbackEngine) {
        this.stockfishAdapter = stockfishAdapter;
        this.fallbackEngine = fallbackEngine;
    }

    public CompletableFuture<Move> calculateBestMove(GameState gameState, int targetElo) {
        long startTime = System.currentTimeMillis();
        Duration timeout = Duration.ofSeconds(3);

        boolean stockfishAvailable = stockfishAdapter.isAvailable();
        CompletableFuture<Move> aiFuture;

        if (stockfishAvailable) {
            log.info("Computing AI move using Stockfish UCI engine (Target ELO: {})", targetElo);
            aiFuture = stockfishAdapter.findBestMove(gameState, targetElo, timeout)
                    .exceptionally(ex -> {
                        log.warn("Stockfish UCI failed ({}), falling back to Java engine", ex.getMessage());
                        return fallbackEngine.findBestMove(gameState, targetElo, timeout).join();
                    });
        } else {
            log.info("Stockfish binary not found. Computing AI move using Java Fallback engine (Target ELO: {})", targetElo);
            aiFuture = fallbackEngine.findBestMove(gameState, targetElo, timeout);
        }

        return aiFuture.thenApply(move -> {
            long elapsed = System.currentTimeMillis() - startTime;
            long minDelayMs = 500;
            if (elapsed < minDelayMs) {
                try {
                    Thread.sleep(minDelayMs - elapsed);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            return move;
        });
    }
}
