package com.jchess.domain.engine;

import com.jchess.domain.model.GameState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerftTest {

    @Test
    @DisplayName("Initial Position Perft 검증 (Depth 1~4)")
    void initialPositionPerft() {
        GameState state = GameState.initial();
        assertThat(PerftCalculator.perft(state, 1)).isEqualTo(20L);
        assertThat(PerftCalculator.perft(state, 2)).isEqualTo(400L);
        assertThat(PerftCalculator.perft(state, 3)).isEqualTo(8902L);
        assertThat(PerftCalculator.perft(state, 4)).isEqualTo(197281L);
    }

    @Test
    @DisplayName("Position 2 (Kiwipete) Perft 검증 (Depth 1~3)")
    void kiwipetePerft() {
        GameState state = GameState.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertThat(PerftCalculator.perft(state, 1)).isEqualTo(48L);
        assertThat(PerftCalculator.perft(state, 2)).isEqualTo(2039L);
        assertThat(PerftCalculator.perft(state, 3)).isEqualTo(97862L);
    }

    @Test
    @DisplayName("Position 3 Perft 검증 (Depth 1~3)")
    void position3Perft() {
        GameState state = GameState.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        assertThat(PerftCalculator.perft(state, 1)).isEqualTo(14L);
        assertThat(PerftCalculator.perft(state, 2)).isEqualTo(191L);
        assertThat(PerftCalculator.perft(state, 3)).isEqualTo(2812L);
    }

    @Test
    @DisplayName("Position 4 Perft 검증 (Depth 1~3)")
    void position4Perft() {
        GameState state = GameState.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1K1R w kq - 0 1");
        assertThat(PerftCalculator.perft(state, 1)).isEqualTo(38L);
        assertThat(PerftCalculator.perft(state, 2)).isEqualTo(1741L);
        assertThat(PerftCalculator.perft(state, 3)).isEqualTo(64027L);
    }

    @Test
    @DisplayName("Position 5 Perft 검증 (Depth 1~3)")
    void position5Perft() {
        GameState state = GameState.fromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        assertThat(PerftCalculator.perft(state, 1)).isEqualTo(44L);
        assertThat(PerftCalculator.perft(state, 2)).isEqualTo(1486L);
        assertThat(PerftCalculator.perft(state, 3)).isEqualTo(62379L);
    }
}
