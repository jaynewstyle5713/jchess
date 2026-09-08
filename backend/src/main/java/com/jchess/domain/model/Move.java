package com.jchess.domain.model;

import java.util.Objects;

/**
 * 체스 수 (Move) 모델.
 * 출발 위치, 도착 위치, 승급 기물 종류를 포함.
 */
public record Move(Position from, Position to, PieceType promotion) {

    public Move {
        Objects.requireNonNull(from, "출발 위치는 null일 수 없습니다.");
        Objects.requireNonNull(to, "도착 위치는 null일 수 없습니다.");
        if (from.equals(to)) {
            throw new IllegalArgumentException("출발 위치와 도착 위치가 동일할 수 없습니다: " + from);
        }
        if (promotion != null && (promotion == PieceType.PAWN || promotion == PieceType.KING)) {
            throw new IllegalArgumentException("폰이나 킹으로는 승급할 수 없습니다: " + promotion);
        }
    }

    public static Move of(Position from, Position to) {
        return new Move(from, to, null);
    }

    public static Move of(Position from, Position to, PieceType promotion) {
        return new Move(from, to, promotion);
    }

    public static Move of(String fromAlgebraic, String toAlgebraic) {
        return of(Position.fromAlgebraic(fromAlgebraic), Position.fromAlgebraic(toAlgebraic));
    }

    public static Move of(String fromAlgebraic, String toAlgebraic, PieceType promotion) {
        return of(Position.fromAlgebraic(fromAlgebraic), Position.fromAlgebraic(toAlgebraic), promotion);
    }

    /**
     * UCI 형식 문자열 파싱 (예: "e2e4", "e7e8q")
     */
    public static Move fromUci(String uci) {
        Objects.requireNonNull(uci, "UCI 문자열은 null일 수 없습니다.");
        if (uci.length() < 4 || uci.length() > 5) {
            throw new IllegalArgumentException("유효하지 않은 UCI 수 형식입니다: " + uci);
        }
        Position from = Position.fromAlgebraic(uci.substring(0, 2));
        Position to = Position.fromAlgebraic(uci.substring(2, 4));

        PieceType promotion = null;
        if (uci.length() == 5) {
            char p = Character.toLowerCase(uci.charAt(4));
            promotion = switch (p) {
                case 'q' -> PieceType.QUEEN;
                case 'r' -> PieceType.ROOK;
                case 'b' -> PieceType.BISHOP;
                case 'n' -> PieceType.KNIGHT;
                default -> throw new IllegalArgumentException("유효하지 않은 승급 기물 문자입니다: " + p);
            };
        }
        return of(from, to, promotion);
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    public String toUci() {
        String uci = from.toAlgebraic() + to.toAlgebraic();
        if (promotion != null) {
            uci += switch (promotion) {
                case QUEEN -> "q";
                case ROOK -> "r";
                case BISHOP -> "b";
                case KNIGHT -> "n";
                default -> "";
            };
        }
        return uci;
    }

    @Override
    public String toString() {
        return toUci();
    }
}
