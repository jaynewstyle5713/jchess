package com.jchess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameStateTest {

    @Test
    @DisplayName("초기 상태 검증")
    void initialState() {
        GameState state = GameState.initial();
        assertThat(state.activeColor()).isEqualTo(PieceColor.WHITE);
        assertThat(state.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(state.halfmoveClock()).isEqualTo(0);
        assertThat(state.fullmoveNumber()).isEqualTo(1);
        assertThat(state.isEnded()).isFalse();
        assertThat(state.getLegalMoves()).hasSize(20);
    }

    @Test
    @DisplayName("수 적용 시 차례, 기보, FEN, 반수 카운트가 정상 갱신된다.")
    void applyMoveProgression() {
        GameState state = GameState.initial();

        GameState state1 = state.applyMove(Move.of("e2", "e4"));
        assertThat(state1.activeColor()).isEqualTo(PieceColor.BLACK);
        assertThat(state1.enPassantTarget()).isEqualTo(Position.fromAlgebraic("e3"));
        assertThat(state1.halfmoveClock()).isEqualTo(0);
        assertThat(state1.fullmoveNumber()).isEqualTo(1);
        assertThat(state1.moveHistory()).containsExactly(Move.of("e2", "e4"));

        GameState state2 = state1.applyMove(Move.of("e7", "e5"));
        assertThat(state2.activeColor()).isEqualTo(PieceColor.WHITE);
        assertThat(state2.enPassantTarget()).isEqualTo(Position.fromAlgebraic("e6"));
        assertThat(state2.fullmoveNumber()).isEqualTo(2);

        GameState state3 = state2.applyMove(Move.of("g1", "f3"));
        assertThat(state3.activeColor()).isEqualTo(PieceColor.BLACK);
        assertThat(state3.enPassantTarget()).isNull();
        assertThat(state3.halfmoveClock()).isEqualTo(1);
    }

    @Test
    @DisplayName("스콜라 메이트(Scholar's Mate)로 4수만에 체크메이트 발생 검증")
    void scholarsMateCheckmate() {
        GameState state = GameState.initial()
                .applyMove(Move.of("e2", "e4"))
                .applyMove(Move.of("e7", "e5"))
                .applyMove(Move.of("d1", "h5"))
                .applyMove(Move.of("b8", "c6"))
                .applyMove(Move.of("f1", "c4"))
                .applyMove(Move.of("g8", "f6"))
                .applyMove(Move.of("h5", "f7"));

        assertThat(state.status()).isEqualTo(GameStatus.CHECKMATE);
        assertThat(state.result()).isEqualTo(GameResult.WHITE_WON);
        assertThat(state.endReason()).isEqualTo(GameEndReason.CHECKMATE);
        assertThat(state.isEnded()).isTrue();
        assertThat(state.getLegalMoves()).isEmpty();

        assertThatThrownBy(() -> state.applyMove(Move.of("a7", "a6")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("스테일메이트(Stalemate) 무승부 발생 검증")
    void stalemate() {
        GameState state = GameState.fromFen("8/8/8/8/8/5k2/5q2/7K w - - 0 1");
        assertThat(state.status()).isEqualTo(GameStatus.STALEMATE);
        assertThat(state.result()).isEqualTo(GameResult.DRAW);
        assertThat(state.endReason()).isEqualTo(GameEndReason.STALEMATE);
        assertThat(state.isEnded()).isTrue();
        assertThat(state.getLegalMoves()).isEmpty();
    }

    @Test
    @DisplayName("기물 부족(Insufficient Material)으로 인한 무승부 판정 검증")
    void insufficientMaterialDraw() {
        GameState kvk = GameState.fromFen("8/8/8/4k3/8/8/4K3/8 w - - 0 1");
        assertThat(kvk.status()).isEqualTo(GameStatus.DRAW);
        assertThat(kvk.endReason()).isEqualTo(GameEndReason.INSUFFICIENT_MATERIAL);

        GameState kbvk = GameState.fromFen("8/8/8/4k3/8/8/4K1B1/8 w - - 0 1");
        assertThat(kbvk.status()).isEqualTo(GameStatus.DRAW);
        assertThat(kbvk.endReason()).isEqualTo(GameEndReason.INSUFFICIENT_MATERIAL);

        GameState knvk = GameState.fromFen("8/8/8/4k3/8/8/4K1N1/8 w - - 0 1");
        assertThat(knvk.status()).isEqualTo(GameStatus.DRAW);
        assertThat(knvk.endReason()).isEqualTo(GameEndReason.INSUFFICIENT_MATERIAL);

        GameState sameColorBishops = GameState.fromFen("5b2/8/8/4k3/8/8/4K3/2B5 w - - 0 1");
        assertThat(sameColorBishops.status()).isEqualTo(GameStatus.DRAW);
        assertThat(sameColorBishops.endReason()).isEqualTo(GameEndReason.INSUFFICIENT_MATERIAL);
    }

    @Test
    @DisplayName("기권(Resignation) 및 시간초과(Timeout) 처리")
    void resignAndTimeout() {
        GameState state = GameState.initial();

        GameState whiteResigned = state.resign(PieceColor.WHITE);
        assertThat(whiteResigned.status()).isEqualTo(GameStatus.RESIGNED);
        assertThat(whiteResigned.result()).isEqualTo(GameResult.BLACK_WON);
        assertThat(whiteResigned.endReason()).isEqualTo(GameEndReason.RESIGNATION);

        GameState blackTimedOut = state.timeout(PieceColor.BLACK);
        assertThat(blackTimedOut.status()).isEqualTo(GameStatus.TIMEOUT);
        assertThat(blackTimedOut.result()).isEqualTo(GameResult.WHITE_WON);
        assertThat(blackTimedOut.endReason()).isEqualTo(GameEndReason.TIMEOUT);
    }

    @Test
    @DisplayName("3회 반복 무승부 청구 가능 여부 확인")
    void threefoldRepetitionClaim() {
        GameState s = GameState.initial()
                .applyMove(Move.of("g1", "f3"))
                .applyMove(Move.of("g8", "f6"))
                .applyMove(Move.of("f3", "g1"))
                .applyMove(Move.of("f6", "g8"))
                .applyMove(Move.of("g1", "f3"))
                .applyMove(Move.of("g8", "f6"))
                .applyMove(Move.of("f3", "g1"))
                .applyMove(Move.of("f6", "g8"));

        assertThat(s.canClaimThreefoldRepetition()).isTrue();
    }
}
