package com.jchess.domain.engine;

import com.jchess.domain.model.GameState;
import com.jchess.domain.model.Move;

import java.util.List;

/**
 * Perft (Performance Test) 계산기.
 * 체스 엔진의 수 생성 및 규칙 정확성을 표준 벤치마크 포지션과 리프 노드 수로 검증.
 */
public final class PerftCalculator {

    private PerftCalculator() {
    }

    public static long perft(GameState state, int depth) {
        if (depth == 0) {
            return 1L;
        }

        List<Move> legalMoves = state.getLegalMoves();
        if (depth == 1) {
            return legalMoves.size();
        }

        long nodes = 0L;
        for (Move move : legalMoves) {
            GameState nextState = state.applyMove(move);
            nodes += perft(nextState, depth - 1);
        }
        return nodes;
    }
}
