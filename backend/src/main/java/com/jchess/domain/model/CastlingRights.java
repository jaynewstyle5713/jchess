package com.jchess.domain.model;

import java.util.Objects;

/**
 * 캐슬링 권리 상태 모델.
 * White/Black의 킹사이드 및 퀸사이드 캐슬링 가능 여부를 관리.
 */
public record CastlingRights(
        boolean whiteKingSide,
        boolean whiteQueenSide,
        boolean blackKingSide,
        boolean blackQueenSide
) {

    public static final CastlingRights ALL = new CastlingRights(true, true, true, true);
    public static final CastlingRights NONE = new CastlingRights(false, false, false, false);

    public static CastlingRights fromFen(String fen) {
        Objects.requireNonNull(fen, "FEN 캐슬링 문자열은 null일 수 없습니다.");
        if (fen.equals("-")) {
            return NONE;
        }
        boolean wk = fen.contains("K");
        boolean wq = fen.contains("Q");
        boolean bk = fen.contains("k");
        boolean bq = fen.contains("q");
        return new CastlingRights(wk, wq, bk, bq);
    }

    public boolean canCastleKingSide(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKingSide : blackKingSide;
    }

    public boolean canCastleQueenSide(PieceColor color) {
        return color == PieceColor.WHITE ? whiteQueenSide : blackQueenSide;
    }

    public CastlingRights withoutWhiteKingSide() {
        return new CastlingRights(false, whiteQueenSide, blackKingSide, blackQueenSide);
    }

    public CastlingRights withoutWhiteQueenSide() {
        return new CastlingRights(whiteKingSide, false, blackKingSide, blackQueenSide);
    }

    public CastlingRights withoutWhite() {
        return new CastlingRights(false, false, blackKingSide, blackQueenSide);
    }

    public CastlingRights withoutBlackKingSide() {
        return new CastlingRights(whiteKingSide, whiteQueenSide, false, blackQueenSide);
    }

    public CastlingRights withoutBlackQueenSide() {
        return new CastlingRights(whiteKingSide, whiteQueenSide, blackKingSide, false);
    }

    public CastlingRights withoutBlack() {
        return new CastlingRights(whiteKingSide, whiteQueenSide, false, false);
    }

    /**
     * 특정 이동에 따라 캐슬링 권리 갱신 (킹 이동, 룩 이동, 룩 포획 등)
     */
    public CastlingRights updateAfterMove(Move move, Piece movedPiece, Piece capturedPiece) {
        CastlingRights updated = this;

        // 1. 킹이 움직인 경우
        if (movedPiece != null && movedPiece.type() == PieceType.KING) {
            if (movedPiece.color() == PieceColor.WHITE) {
                updated = updated.withoutWhite();
            } else {
                updated = updated.withoutBlack();
            }
        }

        // 2. 룩이 원래 자리에서 움직인 경우
        Position from = move.from();
        if (from.equals(Position.of(0, 0))) { // a1 (White QueenSide Rook)
            updated = updated.withoutWhiteQueenSide();
        } else if (from.equals(Position.of(7, 0))) { // h1 (White KingSide Rook)
            updated = updated.withoutWhiteKingSide();
        } else if (from.equals(Position.of(0, 7))) { // a8 (Black QueenSide Rook)
            updated = updated.withoutBlackQueenSide();
        } else if (from.equals(Position.of(7, 7))) { // h8 (Black KingSide Rook)
            updated = updated.withoutBlackKingSide();
        }

        // 3. 룩이 잡힌 경우
        Position to = move.to();
        if (to.equals(Position.of(0, 0))) { // a1 Rook 잡힘
            updated = updated.withoutWhiteQueenSide();
        } else if (to.equals(Position.of(7, 0))) { // h1 Rook 잡힘
            updated = updated.withoutWhiteKingSide();
        } else if (to.equals(Position.of(0, 7))) { // a8 Rook 잡힘
            updated = updated.withoutBlackQueenSide();
        } else if (to.equals(Position.of(7, 7))) { // h8 Rook 잡힘
            updated = updated.withoutBlackKingSide();
        }

        return updated;
    }

    public String toFen() {
        if (!whiteKingSide && !whiteQueenSide && !blackKingSide && !blackQueenSide) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        if (whiteKingSide) sb.append('K');
        if (whiteQueenSide) sb.append('Q');
        if (blackKingSide) sb.append('k');
        if (blackQueenSide) sb.append('q');
        return sb.toString();
    }
}
