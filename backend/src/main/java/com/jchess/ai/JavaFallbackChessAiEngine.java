package com.jchess.ai;

import com.jchess.domain.engine.MoveGenerator;
import com.jchess.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class JavaFallbackChessAiEngine implements ChessAiEngine {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public CompletableFuture<Move> findBestMove(GameState gameState, int targetElo, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            List<Move> legalMoves = MoveGenerator.generateLegalMoves(
                    gameState.board(), gameState.activeColor(), gameState.castlingRights(), gameState.enPassantTarget()
            );
            if (legalMoves.isEmpty()) {
                return null;
            }

            int depth = targetElo >= 2000 ? 3 : 2;
            PieceColor aiColor = gameState.activeColor();

            Move bestMove = legalMoves.get(0);
            int bestScore = Integer.MIN_VALUE;

            for (Move move : legalMoves) {
                GameState nextState = gameState.applyMove(move);
                int score = -alphaBeta(nextState, depth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, aiColor.opposite());
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }

            return bestMove;
        });
    }

    private int alphaBeta(GameState state, int depth, int alpha, int beta, PieceColor currentTurn) {
        if (state.status() == GameStatus.CHECKMATE) {
            return -200000 - depth;
        }
        if (state.status() == GameStatus.STALEMATE || state.status() == GameStatus.DRAW) {
            return 0;
        }
        if (depth == 0) {
            return evaluate(state, currentTurn);
        }

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(
                state.board(), state.activeColor(), state.castlingRights(), state.enPassantTarget()
        );
        if (legalMoves.isEmpty()) {
            return state.isInCheck() ? -200000 - depth : 0;
        }

        for (Move move : legalMoves) {
            GameState nextState = state.applyMove(move);
            int score = -alphaBeta(nextState, depth - 1, -beta, -alpha, currentTurn.opposite());
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    private int evaluate(GameState state, PieceColor perspective) {
        Board board = state.board();
        int whiteMaterial = 0;
        int blackMaterial = 0;

        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = board.getPiece(new Position(file, rank));
                if (piece != null) {
                    int val = getPieceValue(piece.type());
                    int posBonus = getCenterBonus(file, rank);
                    if (piece.color() == PieceColor.WHITE) {
                        whiteMaterial += (val + posBonus);
                    } else {
                        blackMaterial += (val + posBonus);
                    }
                }
            }
        }

        int diff = whiteMaterial - blackMaterial;
        return perspective == PieceColor.WHITE ? diff : -diff;
    }

    private int getPieceValue(PieceType type) {
        return switch (type) {
            case PAWN -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK -> 500;
            case QUEEN -> 900;
            case KING -> 20000;
        };
    }

    private int getCenterBonus(int file, int rank) {
        if ((file == 3 || file == 4) && (rank == 3 || rank == 4)) return 15;
        if ((file >= 2 && file <= 5) && (rank >= 2 && rank <= 5)) return 5;
        return 0;
    }
}

