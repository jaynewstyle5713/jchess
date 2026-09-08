package com.jchess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardTest {

    @Test
    @DisplayName("초기 보드 배치가 정확하다.")
    void initialBoardSetup() {
        Board board = Board.initial();

        // White pieces
        assertThat(board.getPiece(Position.fromAlgebraic("e1"))).isEqualTo(Piece.of(PieceColor.WHITE, PieceType.KING));
        assertThat(board.getPiece(Position.fromAlgebraic("d1"))).isEqualTo(Piece.of(PieceColor.WHITE, PieceType.QUEEN));
        assertThat(board.getPiece(Position.fromAlgebraic("a1"))).isEqualTo(Piece.of(PieceColor.WHITE, PieceType.ROOK));
        assertThat(board.getPiece(Position.fromAlgebraic("e2"))).isEqualTo(Piece.of(PieceColor.WHITE, PieceType.PAWN));

        // Black pieces
        assertThat(board.getPiece(Position.fromAlgebraic("e8"))).isEqualTo(Piece.of(PieceColor.BLACK, PieceType.KING));
        assertThat(board.getPiece(Position.fromAlgebraic("d8"))).isEqualTo(Piece.of(PieceColor.BLACK, PieceType.QUEEN));
        assertThat(board.getPiece(Position.fromAlgebraic("e7"))).isEqualTo(Piece.of(PieceColor.BLACK, PieceType.PAWN));

        // Empty squares
        assertThat(board.isEmpty(Position.fromAlgebraic("e4"))).isTrue();

        // King search
        assertThat(board.findKing(PieceColor.WHITE)).isEqualTo(Position.fromAlgebraic("e1"));
        assertThat(board.findKing(PieceColor.BLACK)).isEqualTo(Position.fromAlgebraic("e8"));

        // FEN export
        assertThat(board.toFen()).isEqualTo("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
    }

    @Test
    @DisplayName("FEN 문자열로부터 보드를 정확히 파싱하고 다시 FEN으로 복원한다.")
    void fenParsing() {
        String fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R";
        Board board = Board.fromFen(fen);
        assertThat(board.toFen()).isEqualTo(fen);

        assertThat(board.getPiece(Position.fromAlgebraic("c4"))).isEqualTo(Piece.of(PieceColor.WHITE, PieceType.BISHOP));
        assertThat(board.getPiece(Position.fromAlgebraic("f6"))).isEqualTo(Piece.of(PieceColor.BLACK, PieceType.KNIGHT));
    }

    @Test
    @DisplayName("기물 추가/제거/이동 시 새로운 불변 Board 객체가 반환된다.")
    void immutableBoardOperations() {
        Board board = Board.empty();
        Position e4 = Position.fromAlgebraic("e4");
        Piece whitePawn = Piece.of(PieceColor.WHITE, PieceType.PAWN);

        Board boardWithPawn = board.withPiece(e4, whitePawn);
        assertThat(board.isEmpty(e4)).isTrue();
        assertThat(boardWithPawn.getPiece(e4)).isEqualTo(whitePawn);

        Board boardWithoutPawn = boardWithPawn.withoutPiece(e4);
        assertThat(boardWithoutPawn.isEmpty(e4)).isTrue();

        Board movedBoard = boardWithPawn.movePiece(e4, Position.fromAlgebraic("e5"));
        assertThat(movedBoard.isEmpty(e4)).isTrue();
        assertThat(movedBoard.getPiece(Position.fromAlgebraic("e5"))).isEqualTo(whitePawn);
    }
}
