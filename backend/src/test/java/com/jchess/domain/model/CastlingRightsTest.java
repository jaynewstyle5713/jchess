package com.jchess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CastlingRightsTest {

    @Test
    @DisplayName("FEN 캐슬링 문자열 파싱 및 직렬화")
    void fenParsing() {
        CastlingRights all = CastlingRights.fromFen("KQkq");
        assertThat(all.whiteKingSide()).isTrue();
        assertThat(all.whiteQueenSide()).isTrue();
        assertThat(all.blackKingSide()).isTrue();
        assertThat(all.blackQueenSide()).isTrue();
        assertThat(all.toFen()).isEqualTo("KQkq");

        CastlingRights none = CastlingRights.fromFen("-");
        assertThat(none).isEqualTo(CastlingRights.NONE);
        assertThat(none.toFen()).isEqualTo("-");

        CastlingRights partial = CastlingRights.fromFen("Kq");
        assertThat(partial.whiteKingSide()).isTrue();
        assertThat(partial.whiteQueenSide()).isFalse();
        assertThat(partial.blackKingSide()).isFalse();
        assertThat(partial.blackQueenSide()).isTrue();
        assertThat(partial.toFen()).isEqualTo("Kq");
    }

    @Test
    @DisplayName("킹 이동 시 해당 색상의 모든 캐슬링 권리가 상실된다.")
    void kingMoveRevokesRights() {
        CastlingRights rights = CastlingRights.ALL;
        Move move = Move.of("e1", "e2");
        Piece king = Piece.of(PieceColor.WHITE, PieceType.KING);

        CastlingRights updated = rights.updateAfterMove(move, king, null);
        assertThat(updated.whiteKingSide()).isFalse();
        assertThat(updated.whiteQueenSide()).isFalse();
        assertThat(updated.blackKingSide()).isTrue();
        assertThat(updated.blackQueenSide()).isTrue();
    }

    @Test
    @DisplayName("룩 이동 또는 룩 포획 시 해당 룩의 캐슬링 권리만 상실된다.")
    void rookMoveOrCaptureRevokesRights() {
        CastlingRights rights = CastlingRights.ALL;
        // White h1 룩 이동
        Move moveH1 = Move.of("h1", "h3");
        Piece rook = Piece.of(PieceColor.WHITE, PieceType.ROOK);
        CastlingRights updatedH1 = rights.updateAfterMove(moveH1, rook, null);
        assertThat(updatedH1.whiteKingSide()).isFalse();
        assertThat(updatedH1.whiteQueenSide()).isTrue();

        // White a1 룩이 Black에게 잡힘
        Move captureA1 = Move.of("b2", "a1");
        CastlingRights updatedA1 = rights.updateAfterMove(captureA1, Piece.of(PieceColor.BLACK, PieceType.QUEEN), Piece.of(PieceColor.WHITE, PieceType.ROOK));
        assertThat(updatedA1.whiteQueenSide()).isFalse();
        assertThat(updatedA1.whiteKingSide()).isTrue();
    }
}
