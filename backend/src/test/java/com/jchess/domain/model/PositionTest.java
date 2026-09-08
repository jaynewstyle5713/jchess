package com.jchess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionTest {

    @Test
    @DisplayName("유효한 좌표로 Position을 생성할 수 있다.")
    void createValidPosition() {
        Position pos = Position.of(4, 3); // e4
        assertThat(pos.file()).isEqualTo(4);
        assertThat(pos.rank()).isEqualTo(3);
        assertThat(pos.toAlgebraic()).isEqualTo("e4");
        assertThat(pos.fileChar()).isEqualTo('e');
        assertThat(pos.rankChar()).isEqualTo('4');
    }

    @ParameterizedTest
    @CsvSource({
            "a1, 0, 0",
            "h1, 7, 0",
            "a8, 0, 7",
            "h8, 7, 7",
            "e4, 4, 3",
            "c6, 2, 5"
    })
    @DisplayName("대수 표기(Algebraic notation)로부터 Position을 파싱할 수 있다.")
    void fromAlgebraic(String algebraic, int expectedFile, int expectedRank) {
        Position pos = Position.fromAlgebraic(algebraic);
        assertThat(pos.file()).isEqualTo(expectedFile);
        assertThat(pos.rank()).isEqualTo(expectedRank);
        assertThat(pos.toAlgebraic()).isEqualTo(algebraic);
    }

    @Test
    @DisplayName("유효하지 않은 좌표는 예외를 던진다.")
    void invalidCoordinates() {
        assertThatThrownBy(() -> Position.of(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.of(8, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.of(0, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic("z9"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.fromAlgebraic("e"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("offset을 통해 새로운 위치를 계산할 수 있으며 판을 벗어나면 null을 반환한다.")
    void offsetPosition() {
        Position e4 = Position.of(4, 3);
        assertThat(e4.offset(0, 1)).isEqualTo(Position.of(4, 4)); // e5
        assertThat(e4.offset(1, 2)).isEqualTo(Position.of(5, 5)); // f6
        assertThat(e4.offset(-1, -1)).isEqualTo(Position.of(3, 2)); // d3

        Position a1 = Position.of(0, 0);
        assertThat(a1.offset(-1, 0)).isNull();
        assertThat(a1.offset(0, -1)).isNull();
    }
}
