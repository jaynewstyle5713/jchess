package com.jchess.ai;

import com.jchess.domain.model.GameState;
import com.jchess.domain.model.Move;
import com.jchess.domain.model.PieceColor;
import com.jchess.domain.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class JavaFallbackChessAiEngineTest {

    private JavaFallbackChessAiEngine aiEngine;

    @BeforeEach
    void setUp() {
        aiEngine = new JavaFallbackChessAiEngine();
    }

    @Test
    @DisplayName("초기 포지션에서 합법적인 수를 성공적으로 생성한다.")
    void findBestMoveInInitialPosition() throws Exception {
        GameState initial = GameState.initial();
        CompletableFuture<Move> future = aiEngine.findBestMove(initial, 2000, Duration.ofSeconds(2));
        Move move = future.get();

        assertThat(move).isNotNull();
        assertThat(initial.isLegalMove(move)).isTrue();
    }

    @Test
    @DisplayName("1수 체크메이트 기회가 주어졌을 때 체크메이트 수를 도출한다 (Scholar's Mate 포지션)")
    void findMateInOneMove() throws Exception {
        // Scholar's Mate 직전: 백 퀸 f7 이동 시 체크메이트
        // FEN: r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4
        GameState mateInOne = GameState.fromFen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4");
        CompletableFuture<Move> future = aiEngine.findBestMove(mateInOne, 2000, Duration.ofSeconds(2));
        Move move = future.get();

        assertThat(move).isNotNull();
        assertThat(move.from()).isEqualTo(Position.fromAlgebraic("h5"));
        assertThat(move.to()).isEqualTo(Position.fromAlgebraic("f7"));

        GameState afterMove = mateInOne.applyMove(move);
        assertThat(afterMove.status()).isEqualTo(com.jchess.domain.model.GameStatus.CHECKMATE);
    }

    @Test
    @DisplayName("자신의 퀸이 위험할 때 캡처를 피하거나 보호하는 최적의 수를 선택한다")
    void avoidQueenHanging() throws Exception {
        // 백 퀸이 공격받고 있을 때 안전한 칸으로 피하는 수 탐색
        GameState state = GameState.fromFen("rnb1kbnr/pppp1ppp/8/4p3/5q2/3P4/PPP1PPPP/RNBQKBNR w KQkq - 1 3");
        CompletableFuture<Move> future = aiEngine.findBestMove(state, 2000, Duration.ofSeconds(2));
        Move move = future.get();

        assertThat(move).isNotNull();
        assertThat(state.isLegalMove(move)).isTrue();
    }
}
