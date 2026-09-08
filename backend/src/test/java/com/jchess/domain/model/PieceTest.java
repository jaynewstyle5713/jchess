package com.jchess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PieceTest {

    @ParameterizedTest
    @CsvSource({
            "P, WHITE, PAWN",
            "N, WHITE, KNIGHT",
            "B, WHITE, BISHOP",
            "R, WHITE, ROOK",
            "Q, WHITE, QUEEN",
            "K, WHITE, KING",
            "p, BLACK, PAWN",
            "n, BLACK, KNIGHT",
            "b, BLACK, BISHOP",
            "r, BLACK, ROOK",
            "q, BLACK, QUEEN",
            "k, BLACK, KING"
    })
    @DisplayName("FEN 문자와 Piece 상호 변환이 정확하다.")
    void fenCharConversion(char fenChar, PieceColor expectedColor, PieceType expectedType) {
        Piece piece = Piece.fromFenChar(fenChar);
        assertThat(piece.color()).isEqualTo(expectedColor);
        assertThat(piece.type()).isEqualTo(expectedType);
        assertThat(piece.toFenChar()).isEqualTo(fenChar);
    }

    @Test
    @DisplayName("잘못된 FEN 문자는 예외를 던진다.")
    void invalidFenChar() {
        assertThatThrownBy(() -> Piece.fromFenChar('x'))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
