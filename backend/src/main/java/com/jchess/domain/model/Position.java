package com.jchess.domain.model;

import java.util.Objects;

/**
 * 8x8 체스판의 위치를 나타내는 불변 불변 값 객체.
 * file: 0 (a) ~ 7 (h)
 * rank: 0 (1) ~ 7 (8)
 */
public record Position(int file, int rank) {

    private static final Position[] CACHE = new Position[64];

    static {
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                CACHE[r * 8 + f] = new Position(f, r);
            }
        }
    }

    public Position {
        if (!isValid(file, rank)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 체스판 위치입니다: file=" + file + ", rank=" + rank
            );
        }
    }

    public static Position of(int file, int rank) {
        if (!isValid(file, rank)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 체스판 위치입니다: file=" + file + ", rank=" + rank
            );
        }
        return CACHE[rank * 8 + file];
    }

    public static Position fromAlgebraic(String algebraic) {
        Objects.requireNonNull(algebraic, "대수 표기는 null일 수 없습니다.");
        if (algebraic.length() != 2) {
            throw new IllegalArgumentException("유효하지 않은 대수 표기 좌표입니다: " + algebraic);
        }
        char fileChar = Character.toLowerCase(algebraic.charAt(0));
        char rankChar = algebraic.charAt(1);

        int file = fileChar - 'a';
        int rank = rankChar - '1';

        if (!isValid(file, rank)) {
            throw new IllegalArgumentException("유효하지 않은 대수 표기 좌표입니다: " + algebraic);
        }
        return of(file, rank);
    }

    public static boolean isValid(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    public Position offset(int dFile, int dRank) {
        int newFile = this.file + dFile;
        int newRank = this.rank + dRank;
        if (!isValid(newFile, newRank)) {
            return null;
        }
        return of(newFile, newRank);
    }

    public char fileChar() {
        return (char) ('a' + file);
    }

    public char rankChar() {
        return (char) ('1' + rank);
    }

    public String toAlgebraic() {
        return "" + fileChar() + rankChar();
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
