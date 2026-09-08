package com.jchess.domain.engine;

import com.jchess.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoveGeneratorTest {

    @Test
    @DisplayName("초기 포지션에서 White의 첫 합법 수는 20개이다 (폰 16개 + 나이트 4개).")
    void initialPositionWhiteMoves() {
        Board board = Board.initial();
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board, PieceColor.WHITE, CastlingRights.ALL, null);
        assertThat(legalMoves).hasSize(20);
    }

    @Test
    @DisplayName("폰의 전진, 2칸 전진, 대각선 포획 검증")
    void pawnMoves() {
        Board board = Board.empty()
                .withPiece(Position.fromAlgebraic("e1"), Piece.of(PieceColor.WHITE, PieceType.KING))
                .withPiece(Position.fromAlgebraic("e8"), Piece.of(PieceColor.BLACK, PieceType.KING))
                .withPiece(Position.fromAlgebraic("e4"), Piece.of(PieceColor.WHITE, PieceType.PAWN))
                .withPiece(Position.fromAlgebraic("d5"), Piece.of(PieceColor.BLACK, PieceType.PAWN));

        List<Move> pawnMoves = MoveGenerator.generateLegalMovesFrom(board, PieceColor.WHITE, CastlingRights.NONE, null, Position.fromAlgebraic("e4"));
        assertThat(pawnMoves).containsExactlyInAnyOrder(
                Move.of("e4", "e5"),
                Move.of("e4", "d5")
        );
    }

    @Test
    @DisplayName("폰 승급 시 Q, R, B, N 4가지 수가 생성된다.")
    void pawnPromotion() {
        Board board = Board.empty()
                .withPiece(Position.fromAlgebraic("e1"), Piece.of(PieceColor.WHITE, PieceType.KING))
                .withPiece(Position.fromAlgebraic("h8"), Piece.of(PieceColor.BLACK, PieceType.KING))
                .withPiece(Position.fromAlgebraic("e7"), Piece.of(PieceColor.WHITE, PieceType.PAWN));

        List<Move> promoMoves = MoveGenerator.generateLegalMovesFrom(board, PieceColor.WHITE, CastlingRights.NONE, null, Position.fromAlgebraic("e7"));
        assertThat(promoMoves).containsExactlyInAnyOrder(
                Move.of("e7", "e8", PieceType.QUEEN),
                Move.of("e7", "e8", PieceType.ROOK),
                Move.of("e7", "e8", PieceType.BISHOP),
                Move.of("e7", "e8", PieceType.KNIGHT)
        );
    }

    @Test
    @DisplayName("앙파상(En passant) 포획 수 생성 및 적용 검증")
    void enPassant() {
        Board board = Board.empty()
                .withPiece(Position.fromAlgebraic("e1"), Piece.of(PieceColor.WHITE, PieceType.KING))
                .withPiece(Position.fromAlgebraic("e8"), Piece.of(PieceColor.BLACK, PieceType.KING))
                .withPiece(Position.fromAlgebraic("e5"), Piece.of(PieceColor.WHITE, PieceType.PAWN))
                .withPiece(Position.fromAlgebraic("d5"), Piece.of(PieceColor.BLACK, PieceType.PAWN));

        Position enPassantTarget = Position.fromAlgebraic("d6");
        List<Move> moves = MoveGenerator.generateLegalMovesFrom(board, PieceColor.WHITE, CastlingRights.NONE, enPassantTarget, Position.fromAlgebraic("e5"));
        assertThat(moves).contains(Move.of("e5", "d6"));

        Board nextBoard = MoveGenerator.applyMoveToBoard(board, Move.of("e5", "d6"), enPassantTarget);
        assertThat(nextBoard.getPiece(Position.fromAlgebraic("d6"))).isEqualTo(Piece.of(PieceColor.WHITE, PieceType.PAWN));
        assertThat(nextBoard.getPiece(Position.fromAlgebraic("d5"))).isNull();
        assertThat(nextBoard.getPiece(Position.fromAlgebraic("e5"))).isNull();
    }

    @Test
    @DisplayName("캐슬링 조건: 체크 상태이거나 통과 칸이 공격받을 경우 캐슬링 불가")
    void castlingRestrictions() {
        Board inCheckBoard = Board.empty()
                .withPiece(Position.fromAlgebraic("e1"), Piece.of(PieceColor.WHITE, PieceType.KING))
                .withPiece(Position.fromAlgebraic("h1"), Piece.of(PieceColor.WHITE, PieceType.ROOK))
                .withPiece(Position.fromAlgebraic("a1"), Piece.of(PieceColor.WHITE, PieceType.ROOK))
                .withPiece(Position.fromAlgebraic("e8"), Piece.of(PieceColor.BLACK, PieceType.ROOK));

        List<Move> kingMovesInCheck = MoveGenerator.generateLegalMovesFrom(inCheckBoard, PieceColor.WHITE, CastlingRights.ALL, null, Position.fromAlgebraic("e1"));
        assertThat(kingMovesInCheck).doesNotContain(Move.of("e1", "g1"), Move.of("e1", "c1"));

        Board transitAttackedBoard = Board.empty()
                .withPiece(Position.fromAlgebraic("e1"), Piece.of(PieceColor.WHITE, PieceType.KING))
                .withPiece(Position.fromAlgebraic("h1"), Piece.of(PieceColor.WHITE, PieceType.ROOK))
                .withPiece(Position.fromAlgebraic("a1"), Piece.of(PieceColor.WHITE, PieceType.ROOK))
                .withPiece(Position.fromAlgebraic("f8"), Piece.of(PieceColor.BLACK, PieceType.ROOK))
                .withPiece(Position.fromAlgebraic("h8"), Piece.of(PieceColor.BLACK, PieceType.KING));

        List<Move> kingMovesTransit = MoveGenerator.generateLegalMovesFrom(transitAttackedBoard, PieceColor.WHITE, CastlingRights.ALL, null, Position.fromAlgebraic("e1"));
        assertThat(kingMovesTransit).doesNotContain(Move.of("e1", "g1"));
        assertThat(kingMovesTransit).contains(Move.of("e1", "c1"));
    }

    @Test
    @DisplayName("핀(Pin)된 기물은 자신의 킹을 노출시키는 이동을 할 수 없다.")
    void pinnedPieceCannotMove() {
        Board board = Board.empty()
                .withPiece(Position.fromAlgebraic("e1"), Piece.of(PieceColor.WHITE, PieceType.KING))
                .withPiece(Position.fromAlgebraic("e2"), Piece.of(PieceColor.WHITE, PieceType.BISHOP))
                .withPiece(Position.fromAlgebraic("e8"), Piece.of(PieceColor.BLACK, PieceType.ROOK));

        List<Move> bishopMoves = MoveGenerator.generateLegalMovesFrom(board, PieceColor.WHITE, CastlingRights.NONE, null, Position.fromAlgebraic("e2"));
        assertThat(bishopMoves).isEmpty();
    }
}
