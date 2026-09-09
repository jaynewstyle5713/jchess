package com.jchess.ai;

import com.jchess.domain.model.GameState;
import com.jchess.domain.model.Move;
import com.jchess.domain.model.PieceType;
import com.jchess.domain.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StockfishUciEngineAdapter implements ChessAiEngine {

    private static final Logger log = LoggerFactory.getLogger(StockfishUciEngineAdapter.class);
    private static final Pattern BEST_MOVE_PATTERN = Pattern.compile("bestmove\\s+([a-h][1-8])([a-h][1-8])([qrbnQRBN])?");

    private final String stockfishPath;

    public StockfishUciEngineAdapter(@Value("${jchess.ai.stockfish-path:stockfish}") String stockfishPath) {
        this.stockfishPath = stockfishPath;
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(stockfishPath).start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                writer.write("uci\n");
                writer.flush();
                String line;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 1000 && (line = reader.readLine()) != null) {
                    if ("uciok".equals(line.trim())) {
                        writer.write("quit\n");
                        writer.flush();
                        process.waitFor(500, TimeUnit.MILLISECONDS);
                        return true;
                    }
                }
            } finally {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            log.debug("Stockfish binary not available at '{}': {}", stockfishPath, e.getMessage());
        }
        return false;
    }

    @Override
    public CompletableFuture<Move> findBestMove(GameState gameState, int targetElo, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            Process process = null;
            try {
                process = new ProcessBuilder(stockfishPath).start();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                writer.write("uci\n");
                writer.write("setoption name UCI_LimitStrength value true\n");
                writer.write("setoption name UCI_Elo value " + Math.max(1350, Math.min(2850, targetElo)) + "\n");
                writer.write("isready\n");
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    if ("readyok".equals(line.trim())) break;
                }

                String fen = gameState.toFen();
                writer.write("position fen " + fen + "\n");
                long moveTimeMs = Math.min(2000, Math.max(300, timeout.toMillis()));
                writer.write("go movetime " + moveTimeMs + "\n");
                writer.flush();

                Move bestMove = null;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = BEST_MOVE_PATTERN.matcher(line);
                    if (matcher.find()) {
                        String fromStr = matcher.group(1);
                        String toStr = matcher.group(2);
                        String promoStr = matcher.group(3);

                        Position from = Position.fromAlgebraic(fromStr);
                        Position to = Position.fromAlgebraic(toStr);
                        PieceType promotion = promoStr != null ? parsePromotion(promoStr) : null;

                        bestMove = new Move(from, to, promotion);
                        break;
                    }
                }

                writer.write("quit\n");
                writer.flush();
                process.waitFor(500, TimeUnit.MILLISECONDS);

                if (bestMove != null) {
                    return bestMove;
                }
                throw new IllegalStateException("Stockfish did not return bestmove");
            } catch (Exception e) {
                throw new RuntimeException("Stockfish execution failed: " + e.getMessage(), e);
            } finally {
                if (process != null) {
                    process.destroyForcibly();
                }
            }
        });
    }

    private PieceType parsePromotion(String s) {
        return switch (s.toLowerCase()) {
            case "q" -> PieceType.QUEEN;
            case "r" -> PieceType.ROOK;
            case "b" -> PieceType.BISHOP;
            case "n" -> PieceType.KNIGHT;
            default -> null;
        };
    }
}
