package com.jchess.domain.engine;

import com.jchess.domain.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MoveGenerator {

    private static final int[][] KNIGHT_OFFSETS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final int[][] BISHOP_DIRS = { {1, 1}, {1, -1}, {-1, 1}, {-1, -1} };
    private static final int[][] ROOK_DIRS = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
    private static final PieceType[] PROMOTIONS = { PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT };

    private MoveGenerator() {}

    public static List<Move> generateLegalMoves(Board board, PieceColor activeColor, CastlingRights castlingRights, Position enPassantTarget) {
        List<Move> pseudo = generatePseudoLegalMoves(board, activeColor, castlingRights, enPassantTarget);
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudo) {
            Board next = applyMoveToBoard(board, m, enPassantTarget);
            Position kingPos = next.findKing(activeColor);
            if (kingPos != null && !isSquareAttacked(kingPos, activeColor.opposite(), next)) {
                legal.add(m);
            }
        }
        return Collections.unmodifiableList(legal);
    }

    public static List<Move> generateLegalMovesFrom(Board board, PieceColor activeColor, CastlingRights castlingRights, Position enPassantTarget, Position from) {
        List<Move> all = generateLegalMoves(board, activeColor, castlingRights, enPassantTarget);
        List<Move> fromMoves = new ArrayList<>();
        for (Move m : all) {
            if (m.from().equals(from)) fromMoves.add(m);
        }
        return Collections.unmodifiableList(fromMoves);
    }

    public static List<Move> generatePseudoLegalMoves(Board board, PieceColor activeColor, CastlingRights castlingRights, Position enPassantTarget) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Position from = Position.of(f, r);
                Piece p = board.getPiece(from);
                if (p != null && p.color() == activeColor) {
                    generatePieceMoves(board, p, from, castlingRights, enPassantTarget, moves);
                }
            }
        }
        return moves;
    }

    private static void generatePieceMoves(Board b, Piece p, Position from, CastlingRights cr, Position ep, List<Move> moves) {
        switch (p.type()) {
            case PAWN -> generatePawnMoves(b, p.color(), from, ep, moves);
            case KNIGHT -> generateKnightMoves(b, p.color(), from, moves);
            case BISHOP -> generateRayMoves(b, p.color(), from, BISHOP_DIRS, moves);
            case ROOK -> generateRayMoves(b, p.color(), from, ROOK_DIRS, moves);
            case QUEEN -> {
                generateRayMoves(b, p.color(), from, BISHOP_DIRS, moves);
                generateRayMoves(b, p.color(), from, ROOK_DIRS, moves);
            }
            case KING -> generateKingMoves(b, p.color(), from, cr, moves);
        }
    }

    private static void generatePawnMoves(Board b, PieceColor color, Position from, Position ep, List<Move> moves) {
        int dir = color == PieceColor.WHITE ? 1 : -1;
        int startRank = color == PieceColor.WHITE ? 1 : 6;
        int promoRank = color == PieceColor.WHITE ? 7 : 0;

        Position one = from.offset(0, dir);
        if (one != null && b.isEmpty(one)) {
            if (one.rank() == promoRank) {
                for (PieceType promo : PROMOTIONS) moves.add(Move.of(from, one, promo));
            } else {
                moves.add(Move.of(from, one));
                if (from.rank() == startRank) {
                    Position two = from.offset(0, dir * 2);
                    if (two != null && b.isEmpty(two)) moves.add(Move.of(from, two));
                }
            }
        }

        for (int dFile : new int[]{-1, 1}) {
            Position cap = from.offset(dFile, dir);
            if (cap != null) {
                Piece target = b.getPiece(cap);
                if (target != null && target.color() != color) {
                    if (cap.rank() == promoRank) {
                        for (PieceType promo : PROMOTIONS) moves.add(Move.of(from, cap, promo));
                    } else {
                        moves.add(Move.of(from, cap));
                    }
                } else if (cap.equals(ep)) {
                    moves.add(Move.of(from, cap));
                }
            }
        }
    }

    private static void generateKnightMoves(Board b, PieceColor color, Position from, List<Move> moves) {
        for (int[] off : KNIGHT_OFFSETS) {
            Position to = from.offset(off[0], off[1]);
            if (to != null) {
                Piece target = b.getPiece(to);
                if (target == null || target.color() != color) moves.add(Move.of(from, to));
            }
        }
    }

    private static void generateRayMoves(Board b, PieceColor color, Position from, int[][] dirs, List<Move> moves) {
        for (int[] dir : dirs) {
            int step = 1;
            while (true) {
                Position to = from.offset(dir[0] * step, dir[1] * step);
                if (to == null) break;
                Piece target = b.getPiece(to);
                if (target == null) {
                    moves.add(Move.of(from, to));
                } else {
                    if (target.color() != color) moves.add(Move.of(from, to));
                    break;
                }
                step++;
            }
        }
    }

    private static void generateKingMoves(Board b, PieceColor color, Position from, CastlingRights cr, List<Move> moves) {
        for (int[] off : KING_OFFSETS) {
            Position to = from.offset(off[0], off[1]);
            if (to != null) {
                Piece target = b.getPiece(to);
                if (target == null || target.color() != color) moves.add(Move.of(from, to));
            }
        }
        if (cr == null) return;
        int rank = color == PieceColor.WHITE ? 0 : 7;
        Position kingStart = Position.of(4, rank);
        if (!from.equals(kingStart)) return;

        PieceColor opp = color.opposite();
        if (isSquareAttacked(kingStart, opp, b)) return;

        if (cr.canCastleKingSide(color)) {
            Position f = Position.of(5, rank);
            Position g = Position.of(6, rank);
            Piece rook = b.getPiece(Position.of(7, rank));
            if (rook != null && rook.type() == PieceType.ROOK && rook.color() == color
                    && b.isEmpty(f) && b.isEmpty(g)
                    && !isSquareAttacked(f, opp, b) && !isSquareAttacked(g, opp, b)) {
                moves.add(Move.of(kingStart, g));
            }
        }
        if (cr.canCastleQueenSide(color)) {
            Position d = Position.of(3, rank);
            Position c = Position.of(2, rank);
            Position bPos = Position.of(1, rank);
            Piece rook = b.getPiece(Position.of(0, rank));
            if (rook != null && rook.type() == PieceType.ROOK && rook.color() == color
                    && b.isEmpty(d) && b.isEmpty(c) && b.isEmpty(bPos)
                    && !isSquareAttacked(d, opp, b) && !isSquareAttacked(c, opp, b)) {
                moves.add(Move.of(kingStart, c));
            }
        }
    }

    public static boolean isSquareAttacked(Position target, PieceColor attackerColor, Board board) {
        int pawnDir = attackerColor == PieceColor.WHITE ? 1 : -1;
        for (int dFile : new int[]{-1, 1}) {
            Position pPos = target.offset(dFile, -pawnDir);
            if (pPos != null) {
                Piece p = board.getPiece(pPos);
                if (p != null && p.color() == attackerColor && p.type() == PieceType.PAWN) return true;
            }
        }
        for (int[] off : KNIGHT_OFFSETS) {
            Position nPos = target.offset(off[0], off[1]);
            if (nPos != null) {
                Piece p = board.getPiece(nPos);
                if (p != null && p.color() == attackerColor && p.type() == PieceType.KNIGHT) return true;
            }
        }
        for (int[] off : KING_OFFSETS) {
            Position kPos = target.offset(off[0], off[1]);
            if (kPos != null) {
                Piece p = board.getPiece(kPos);
                if (p != null && p.color() == attackerColor && p.type() == PieceType.KING) return true;
            }
        }
        for (int[] dir : BISHOP_DIRS) {
            int step = 1;
            while (true) {
                Position rayPos = target.offset(dir[0] * step, dir[1] * step);
                if (rayPos == null) break;
                Piece p = board.getPiece(rayPos);
                if (p != null) {
                    if (p.color() == attackerColor && (p.type() == PieceType.BISHOP || p.type() == PieceType.QUEEN)) return true;
                    break;
                }
                step++;
            }
        }
        for (int[] dir : ROOK_DIRS) {
            int step = 1;
            while (true) {
                Position rayPos = target.offset(dir[0] * step, dir[1] * step);
                if (rayPos == null) break;
                Piece p = board.getPiece(rayPos);
                if (p != null) {
                    if (p.color() == attackerColor && (p.type() == PieceType.ROOK || p.type() == PieceType.QUEEN)) return true;
                    break;
                }
                step++;
            }
        }
        return false;
    }

    public static Board applyMoveToBoard(Board board, Move move, Position enPassantTarget) {
        Position from = move.from();
        Position to = move.to();
        Piece piece = board.getPiece(from);
        if (piece == null) throw new IllegalArgumentException("출발 칸에 기물이 없습니다: " + from);

        Board nextBoard = board.withoutPiece(from);
        if (piece.type() == PieceType.PAWN && to.equals(enPassantTarget)) {
            nextBoard = nextBoard.withoutPiece(Position.of(to.file(), from.rank()));
        }
        if (piece.type() == PieceType.KING && Math.abs(to.file() - from.file()) == 2) {
            int rank = from.rank();
            if (to.file() == 6) {
                nextBoard = nextBoard.withoutPiece(Position.of(7, rank)).withPiece(Position.of(5, rank), Piece.of(piece.color(), PieceType.ROOK));
            } else if (to.file() == 2) {
                nextBoard = nextBoard.withoutPiece(Position.of(0, rank)).withPiece(Position.of(3, rank), Piece.of(piece.color(), PieceType.ROOK));
            }
        }
        Piece placed = move.promotion() != null ? Piece.of(piece.color(), move.promotion()) : piece;
        return nextBoard.withPiece(to, placed);
    }
}


