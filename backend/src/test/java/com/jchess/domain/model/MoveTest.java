package com.jchess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveTest {

    @Test
    @DisplayName("일반 이동 및 승급 이동을 생성하고 UCI 문자열과 상호 변환한다.")
    void uciConversion() {
        Move normalMove = Move.of("e2", "e4");
        assertThat(normalMove.toUci()).isEqualTo("e2e4");
        assertThat(normalMove.isPromotion()).isFalse();
        assertThat(Move.fromUci("e2e4")).isEqualTo(normalMove);

        Move promoMove = Move.of("e7", "e8", PieceType.QUEEN);
        assertThat(promoMove.toUci()).isEqualTo("e7e8q");
        assertThat(promoMove.isPromotion()).isTrue();
        assertThat(Move.fromUci("e7e8q")).isEqualTo(promoMove);
    }

    @Test
    @DisplayName("출발지와 목적지가 동일한 수는 예외를 던진다.")
    void sameFromAndTo() {
        assertThatThrownBy(() -> Move.of("e4", "e4"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("폰이나 킹으로의 승급은 거부된다.")
    void invalidPromotionPiece() {
        assertThatThrownBy(() -> Move.of("e7", "e8", PieceType.PAWN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Move.of("e7", "e8", PieceType.KING))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
