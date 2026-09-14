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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
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

    private String resolveExecutablePath() {
        if (!"stockfish".equalsIgnoreCase(stockfishPath) && !"".equals(stockfishPath.trim())) {
            return stockfishPath;
        }

        if (checkExecutable("stockfish")) {
            return "stockfish";
        }

        List<String> candidates = List.of(
                "C:\\tools\\stockfish\\stockfish.exe",
                "C:\\Program Files\\Stockfish\\stockfish.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Stockfish.Stockfish_Microsoft.Winget.Source_8wekyb3d8bbwe\\stockfish\\stockfish-windows-x86-64-universal.exe"
        );

        for (String candidate : candidates) {
            if (Files.exists(Paths.get(candidate)) && checkExecutable(candidate)) {
                return candidate;
            }
        }

        return stockfishPath;
    }

    private boolean checkExecutable(String path) {
        try {
            Process process = new ProcessBuilder(path).start();
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
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        String resolved = resolveExecutablePath();
        boolean available = checkExecutable(resolved);
        if (available) {
            log.info("[STOCKFISH_ADAPTER] Stockfish 19 UCI engine verified at '{}'", resolved);
        }
        return available;
    }

    @Override
    public CompletableFuture<Move> findBestMove(GameState gameState, int targetElo, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            String executable = resolveExecutablePath();
            Process process = null;
            try {
                process = new ProcessBuilder(executable).start();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // Stockfish 19 Engine Level & Rating Fine-Tuning
                int skillLevel;
                int uciElo;
                boolean limitStrength;
                long moveTimeMs;

                if (targetElo <= 750) { // Stockfish 8 (입문 ELO 600)
                    skillLevel = 2;
                    limitStrength = true;
                    uciElo = 1320;
                    moveTimeMs = 100;
                } else if (targetElo <= 1050) { // Stockfish 11 (초급 ELO 900)
                    skillLevel = 6;
                    limitStrength = true;
                    uciElo = 1350;
                    moveTimeMs = 150;
                } else if (targetElo <= 1450) { // Stockfish 14 (중급 ELO 1300)
                    skillLevel = 11;
                    limitStrength = true;
                    uciElo = 1500;
                    moveTimeMs = 250;
                } else if (targetElo <= 1850) { // Stockfish 17 (고급 ELO 1700 - 공인 1700 대회 입상자 수준)
                    skillLevel = 18;
                    limitStrength = true;
                    uciElo = 2150;
                    moveTimeMs = 600;
                } else { // Stockfish 19 (초고수 ELO 2000+ - NNUE Master 풀파워)
                    skillLevel = 20;
                    limitStrength = false;
                    uciElo = 3190;
                    moveTimeMs = 800;
                }

                log.info("[STOCKFISH_ENGINE] Move calculation for target ELO {}: skillLevel={}, limitStrength={}, uciElo={}, movetime={}ms",
                        targetElo, skillLevel, limitStrength, uciElo, moveTimeMs);

                writer.write("uci\n");
                writer.write("setoption name Skill Level value " + skillLevel + "\n");
                writer.write("setoption name UCI_LimitStrength value " + limitStrength + "\n");
                if (limitStrength) {
                    writer.write("setoption name UCI_Elo value " + uciElo + "\n");
                }
                writer.write("isready\n");
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    if ("readyok".equals(line.trim())) break;
                }

                String fen = gameState.toFen();
                writer.write("position fen " + fen + "\n");
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
                    log.info("[STOCKFISH_19] Best move found: {} -> {} (promotion={})",
                            bestMove.from().toAlgebraic(), bestMove.to().toAlgebraic(), bestMove.promotion());
                    return bestMove;
                }
                throw new IllegalStateException("Stockfish did not return bestmove");
            } catch (Exception e) {
                log.error("[STOCKFISH_19] Execution error: {}", e.getMessage(), e);
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
