package com.jchess.domain.engine;

import com.jchess.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChessRuleEdgeCasesTest {

    @Test
    @DisplayName("규칙 픽스처 1: Castling-through-check - 킹이 통과하는 칸이 공격받으면 캐슬링 불가")
    void castlingThroughCheckFixture() {
        // White: King on e1, Rooks on a1 and h1.
        // Black: Rook on d8 (attacks d1), Bishop on a6 (attacks f1).
        // e1->c1 passes through d1 (attacked by Rook). e1->g1 passes through f1 (attacked by Bishop).
        GameState state = GameState.fromFen("3r3k/8/b7/8/8/8/8/R3K2R w KQ - 0 1");

        // Both O-O (e1g1) and O-O-O (e1c1) should NOT be legal moves
        List<Move> legalMoves = state.getLegalMoves();
        assertThat(legalMoves).doesNotContain(
                Move.of("e1", "g1"),
                Move.of("e1", "c1")
        );
    }

    @Test
    @DisplayName("규칙 픽스처 2: En passant expiry - 상대 폰의 2칸 이동 직후 다음 1수에만 유효하고 즉시 만료됨")
    void enPassantExpiryFixture() {
        // White Pawn on e5. Black Pawn on d7.
        // 1... d7d5 -> en passant target becomes d6
        GameState s0 = GameState.fromFen("rnbqkbnr/pppp1ppp/8/4P3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1");
        GameState s1 = s0.applyMove(Move.of("d7", "d5"));
        assertThat(s1.enPassantTarget()).isEqualTo(Position.fromAlgebraic("d6"));
        assertThat(s1.isLegalMove(Move.of("e5", "d6"))).isTrue();

        // White does another move instead (e.g. h2h3)
        GameState s2 = s1.applyMove(Move.of("h2", "h3"));

        // Black plays a neutral move (e.g. h7h6)
        GameState s3 = s2.applyMove(Move.of("h7", "h6"));

        // Now White can no longer play e5d6 en passant
        assertThat(s3.enPassantTarget()).isNull();
        assertThat(s3.isLegalMove(Move.of("e5", "d6"))).isFalse();
    }

    @Test
    @DisplayName("규칙 픽스처 3: Promotion with check - 폰 승급과 동시에 상대 킹에게 체크 발생")
    void promotionWithCheckFixture() {
        // White Pawn on e7, Black King on e8. White plays e7e8q+ or e7e8r+
        // Wait, if King is on e8, e7->e8 is capture of king (illegal in standard chess).
        // Let's place Black King on d8, and White Pawn on e7.
        // e7->e8=Q checks Black King on d8!
        GameState state = GameState.fromFen("3k4/4P3/8/8/8/8/8/4K3 w - - 0 1");
        Move promoCheckMove = Move.of("e7", "e8", PieceType.QUEEN);
        assertThat(state.isLegalMove(promoCheckMove)).isTrue();

        GameState nextState = state.applyMove(promoCheckMove);
        assertThat(nextState.status()).isEqualTo(GameStatus.CHECK);
        assertThat(nextState.isInCheck()).isTrue();
    }

    @Test
    @DisplayName("규칙 픽스처 4: Stalemate - 킹이 체크가 아니면서 합법 수가 0개일 때 무승부")
    void stalemateFixture() {
        // White King on a8, Black King on c7, Black Queen on c8 (Wait, c8 gives check).
        // Standard Stalemate: White King on a8, Black Queen on b6, Black King on c7.
        GameState state = GameState.fromFen("k7/2K5/1Q6/8/8/8/8/8 b - - 0 1");
        assertThat(state.status()).isEqualTo(GameStatus.STALEMATE);
        assertThat(state.result()).isEqualTo(GameResult.DRAW);
        assertThat(state.endReason()).isEqualTo(GameEndReason.STALEMATE);
        assertThat(state.getLegalMoves()).isEmpty();
    }

    @Test
    @DisplayName("규칙 픽스처 5: Checkmate - 킹이 체크 상태이면서 합법 수가 0개일 때 패배")
    void checkmateFixture() {
        // Fool's Mate: 1. f3 e5 2. g4 Qh4#
        GameState state = GameState.initial()
                .applyMove(Move.of("f2", "f3"))
                .applyMove(Move.of("e7", "e5"))
                .applyMove(Move.of("g2", "g4"))
                .applyMove(Move.of("d8", "h4")); // Qh4#

        assertThat(state.status()).isEqualTo(GameStatus.CHECKMATE);
        assertThat(state.result()).isEqualTo(GameResult.BLACK_WON);
        assertThat(state.endReason()).isEqualTo(GameEndReason.CHECKMATE);
        assertThat(state.getLegalMoves()).isEmpty();
    }
}
