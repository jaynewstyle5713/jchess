package com.jchess.domain.model;

import java.util.*;

/**
 * 8x8 체스판의 기물 배치를 나타내는 불변 모델.
 */
public final class Board {

    private final Piece[] squares;

    private Board(Piece[] squares) {
        this.squares = squares;
    }

    public static Board empty() {
        return new Board(new Piece[64]);
    }

    public static Board initial() {
        return fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
    }

    public static Board fromFen(String boardFen) {
        Objects.requireNonNull(boardFen, "FEN 보드 문자열은 null일 수 없습니다.");
        Piece[] squares = new Piece[64];
        String[] ranks = boardFen.split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN 보드는 8개의 랭크로 구성되어야 합니다: " + boardFen);
        }

        for (int r = 0; r < 8; r++) {
            String rankStr = ranks[r];
            int rank = 7 - r; // FEN은 8번째 랭크부터 시작 (rank index 7)
            int file = 0;
            for (int i = 0; i < rankStr.length(); i++) {
                char c = rankStr.charAt(i);
                if (Character.isDigit(c)) {
                    file += (c - '0');
                } else {
                    if (file >= 8) {
                        throw new IllegalArgumentException("FEN 랭크의 칸 수가 8을 초과했습니다: " + rankStr);
                    }
                    squares[rank * 8 + file] = Piece.fromFenChar(c);
                    file++;
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("FEN 랭크의 칸 수가 8이 아닙니다: " + rankStr);
            }
        }

        return new Board(squares);
    }

    public Piece getPiece(Position position) {
        Objects.requireNonNull(position, "위치는 null일 수 없습니다.");
        return squares[position.rank() * 8 + position.file()];
    }

    public Piece getPiece(int file, int rank) {
        if (!Position.isValid(file, rank)) {
            return null;
        }
        return squares[rank * 8 + file];
    }

    public boolean isEmpty(Position position) {
        return getPiece(position) == null;
    }

    public Board withPiece(Position position, Piece piece) {
        Objects.requireNonNull(position, "위치는 null일 수 없습니다.");
        Piece[] newSquares = Arrays.copyOf(squares, 64);
        newSquares[position.rank() * 8 + position.file()] = piece;
        return new Board(newSquares);
    }

    public Board withoutPiece(Position position) {
        return withPiece(position, null);
    }

    public Board movePiece(Position from, Position to) {
        Piece piece = getPiece(from);
        if (piece == null) {
            throw new IllegalArgumentException("출발 칸에 기물이 없습니다: " + from);
        }
        Piece[] newSquares = Arrays.copyOf(squares, 64);
        newSquares[from.rank() * 8 + from.file()] = null;
        newSquares[to.rank() * 8 + to.file()] = piece;
        return new Board(newSquares);
    }

    public Position findKing(PieceColor color) {
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = squares[rank * 8 + file];
                if (piece != null && piece.type() == PieceType.KING && piece.color() == color) {
                    return Position.of(file, rank);
                }
            }
        }
        return null;
    }

    public Map<Position, Piece> getAllPieces() {
        Map<Position, Piece> result = new HashMap<>();
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = squares[rank * 8 + file];
                if (piece != null) {
                    result.put(Position.of(file, rank), piece);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public List<Position> getPiecePositions(PieceColor color) {
        List<Position> positions = new ArrayList<>();
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = squares[rank * 8 + file];
                if (piece != null && piece.color() == color) {
                    positions.add(Position.of(file, rank));
                }
            }
        }
        return Collections.unmodifiableList(positions);
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = squares[rank * 8 + file];
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    sb.append(piece.toFenChar());
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount);
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return Arrays.equals(squares, board.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(squares);
    }

    @Override
    public String toString() {
        return toFen();
    }
}
