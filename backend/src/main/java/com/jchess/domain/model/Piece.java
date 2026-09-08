package com.jchess.domain.model;

import java.util.Objects;

/**
 * 체스 기물 모델 (색상과 종류)
 */
public record Piece(PieceColor color, PieceType type) {

    public Piece {
        Objects.requireNonNull(color, "기물 색상은 null일 수 없습니다.");
        Objects.requireNonNull(type, "기물 종류는 null일 수 없습니다.");
    }

    public static Piece of(PieceColor color, PieceType type) {
        return new Piece(color, type);
    }

    public static Piece fromFenChar(char c) {
        PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
        char lower = Character.toLowerCase(c);
        PieceType type = switch (lower) {
            case 'p' -> PieceType.PAWN;
            case 'n' -> PieceType.KNIGHT;
            case 'b' -> PieceType.BISHOP;
            case 'r' -> PieceType.ROOK;
            case 'q' -> PieceType.QUEEN;
            case 'k' -> PieceType.KING;
            default -> throw new IllegalArgumentException("유효하지 않은 FEN 기물 문자입니다: " + c);
        };
        return of(color, type);
    }

    public char toFenChar() {
        char c = switch (type) {
            case PAWN -> 'p';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case ROOK -> 'r';
            case QUEEN -> 'q';
            case KING -> 'k';
        };
        return color == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }

    public boolean is(PieceColor color) {
        return this.color == color;
    }

    public boolean is(PieceType type) {
        return this.type == type;
    }
}
