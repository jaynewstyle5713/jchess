package com.jchess.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockfishUciEngineAdapterTest {

    @Test
    @DisplayName("존재하지 않는 바이너리 경로가 주어지면 isAvailable()이 false를 반환한다.")
    void isAvailableReturnsFalseWhenBinaryNotFound() {
        StockfishUciEngineAdapter adapter = new StockfishUciEngineAdapter("non_existent_stockfish_executable_12345");
        assertThat(adapter.isAvailable()).isFalse();
    }
}
