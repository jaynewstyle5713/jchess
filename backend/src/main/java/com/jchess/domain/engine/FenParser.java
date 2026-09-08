package com.jchess.domain.engine;

import com.jchess.domain.model.*;

import java.util.Objects;

/**
 * FEN (Forsyth–Edwards Notation) 파서 및 직렬화 유틸리티.
 */
public final class FenParser {

    public static final String INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private FenParser() {
    }

    public static GameState parse(String fen) {
        Objects.requireNonNull(fen, "FEN 문자열은 null일 수 없습니다.");
        String[] tokens = fen.trim().split("\\s+");
        if (tokens.length < 4) {
            throw new IllegalArgumentException("FEN 형식 오류: 최소 4개 토큰(기물, 차례, 캐슬링, 앙파상)이 필요합니다: " + fen);
        }

        Board board = Board.fromFen(tokens[0]);

        PieceColor activeColor = switch (tokens[1].toLowerCase()) {
            case "w" -> PieceColor.WHITE;
            case "b" -> PieceColor.BLACK;
            default -> throw new IllegalArgumentException("유효하지 않은 FEN 차례: " + tokens[1]);
        };

        CastlingRights castlingRights = CastlingRights.fromFen(tokens[2]);

        Position enPassantTarget = null;
        if (!tokens[3].equals("-")) {
            enPassantTarget = Position.fromAlgebraic(tokens[3]);
        }

        int halfmoveClock = 0;
        if (tokens.length > 4) {
            try {
                halfmoveClock = Integer.parseInt(tokens[4]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("유효하지 않은 FEN 반수 카운트: " + tokens[4]);
            }
        }

        int fullmoveNumber = 1;
        if (tokens.length > 5) {
            try {
                fullmoveNumber = Integer.parseInt(tokens[5]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("유효하지 않은 FEN 전체 수 번호: " + tokens[5]);
            }
        }

        return GameState.of(board, activeColor, castlingRights, enPassantTarget, halfmoveClock, fullmoveNumber);
    }

    public static String toFen(GameState state) {
        Objects.requireNonNull(state, "GameState는 null일 수 없습니다.");
        String boardFen = state.board().toFen();
        String colorFen = state.activeColor() == PieceColor.WHITE ? "w" : "b";
        String castlingFen = state.castlingRights().toFen();
        String epFen = state.enPassantTarget() != null ? state.enPassantTarget().toAlgebraic() : "-";
        int halfmove = state.halfmoveClock();
        int fullmove = state.fullmoveNumber();

        return String.format("%s %s %s %s %d %d", boardFen, colorFen, castlingFen, epFen, halfmove, fullmove);
    }
}
