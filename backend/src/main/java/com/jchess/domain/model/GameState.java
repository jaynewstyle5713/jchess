package com.jchess.domain.model;

import com.jchess.domain.engine.FenParser;
import com.jchess.domain.engine.MoveGenerator;

import java.util.*;

public final class GameState {

    private final Board board;
    private final PieceColor activeColor;
    private final CastlingRights castlingRights;
    private final Position enPassantTarget;
    private final int halfmoveClock;
    private final int fullmoveNumber;
    private final GameStatus status;
    private final GameResult result;
    private final GameEndReason endReason;
    private final List<String> positionHistory;
    private final List<Move> moveHistory;

    public GameState(
            Board board,
            PieceColor activeColor,
            CastlingRights castlingRights,
            Position enPassantTarget,
            int halfmoveClock,
            int fullmoveNumber,
            GameStatus status,
            GameResult result,
            GameEndReason endReason,
            List<String> positionHistory,
            List<Move> moveHistory
    ) {
        this.board = Objects.requireNonNull(board, "보드는 null일 수 없습니다.");
        this.activeColor = Objects.requireNonNull(activeColor, "현재 차례는 null일 수 없습니다.");
        this.castlingRights = Objects.requireNonNull(castlingRights, "캐슬링 권리는 null일 수 없습니다.");
        this.enPassantTarget = enPassantTarget;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
        this.status = Objects.requireNonNull(status, "게임 상태는 null일 수 없습니다.");
        this.result = result;
        this.endReason = endReason;
        this.positionHistory = positionHistory != null ? List.copyOf(positionHistory) : List.of();
        this.moveHistory = moveHistory != null ? List.copyOf(moveHistory) : List.of();
    }

    public static GameState initial() {
        return fromFen(FenParser.INITIAL_FEN);
    }

    public static GameState fromFen(String fen) {
        return FenParser.parse(fen);
    }

    public static GameState of(
            Board board,
            PieceColor activeColor,
            CastlingRights castlingRights,
            Position enPassantTarget,
            int halfmoveClock,
            int fullmoveNumber
    ) {
        String posKey = createPositionKey(board, activeColor, castlingRights, enPassantTarget);
        List<String> history = List.of(posKey);

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board, activeColor, castlingRights, enPassantTarget);
        Position kingPos = board.findKing(activeColor);
        boolean inCheck = kingPos != null && MoveGenerator.isSquareAttacked(kingPos, activeColor.opposite(), board);

        GameStatus status;
        GameResult result = null;
        GameEndReason endReason = null;

        if (legalMoves.isEmpty()) {
            if (inCheck) {
                status = GameStatus.CHECKMATE;
                result = activeColor == PieceColor.WHITE ? GameResult.BLACK_WON : GameResult.WHITE_WON;
                endReason = GameEndReason.CHECKMATE;
            } else {
                status = GameStatus.STALEMATE;
                result = GameResult.DRAW;
                endReason = GameEndReason.STALEMATE;
            }
        } else if (isInsufficientMaterial(board)) {
            status = GameStatus.DRAW;
            result = GameResult.DRAW;
            endReason = GameEndReason.INSUFFICIENT_MATERIAL;
        } else if (inCheck) {
            status = GameStatus.CHECK;
        } else {
            status = GameStatus.ACTIVE;
        }

        return new GameState(
                board, activeColor, castlingRights, enPassantTarget,
                halfmoveClock, fullmoveNumber, status, result, endReason, history, List.of()
        );
    }

    public boolean isEnded() {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE
                || status == GameStatus.DRAW || status == GameStatus.RESIGNED || status == GameStatus.TIMEOUT;
    }

    public boolean isInCheck() {
        Position kingPos = board.findKing(activeColor);
        return kingPos != null && MoveGenerator.isSquareAttacked(kingPos, activeColor.opposite(), board);
    }

    public List<Move> getLegalMoves() {
        if (isEnded()) return List.of();
        return MoveGenerator.generateLegalMoves(board, activeColor, castlingRights, enPassantTarget);
    }

    public List<Move> getLegalMovesFrom(Position from) {
        if (isEnded()) return List.of();
        return MoveGenerator.generateLegalMovesFrom(board, activeColor, castlingRights, enPassantTarget, from);
    }

    public boolean isLegalMove(Move move) {
        return getLegalMoves().contains(move);
    }

    public String toFen() {
        return FenParser.toFen(this);
    }

    public GameState applyMove(Move move) {
        if (isEnded()) throw new IllegalStateException("이미 종료된 대국에는 수를 둘 수 없습니다.");
        if (!isLegalMove(move)) throw new IllegalArgumentException("합법적인 수가 아닙니다: " + move);

        Piece piece = board.getPiece(move.from());
        Piece capturedPiece = board.getPiece(move.to());
        boolean isEp = piece.type() == PieceType.PAWN && move.to().equals(enPassantTarget);
        if (isEp) {
            capturedPiece = board.getPiece(Position.of(move.to().file(), move.from().rank()));
        }

        Board nextBoard = MoveGenerator.applyMoveToBoard(board, move, enPassantTarget);
        CastlingRights nextCastlingRights = castlingRights.updateAfterMove(move, piece, capturedPiece);

        Position nextEp = null;
        if (piece.type() == PieceType.PAWN && Math.abs(move.to().rank() - move.from().rank()) == 2) {
            nextEp = Position.of(move.from().file(), (move.from().rank() + move.to().rank()) / 2);
        }

        boolean isPawnMove = piece.type() == PieceType.PAWN;
        boolean isCapture = capturedPiece != null || isEp;
        int nextHalfmove = (isPawnMove || isCapture) ? 0 : halfmoveClock + 1;

        int nextFullmove = activeColor == PieceColor.BLACK ? fullmoveNumber + 1 : fullmoveNumber;
        PieceColor nextColor = activeColor.opposite();

        String posKey = createPositionKey(nextBoard, nextColor, nextCastlingRights, nextEp);
        List<String> nextHistory = new ArrayList<>(positionHistory);
        nextHistory.add(posKey);

        List<Move> nextMoveHistory = new ArrayList<>(moveHistory);
        nextMoveHistory.add(move);

        List<Move> opponentLegalMoves = MoveGenerator.generateLegalMoves(nextBoard, nextColor, nextCastlingRights, nextEp);
        Position nextKingPos = nextBoard.findKing(nextColor);
        boolean inCheck = nextKingPos != null && MoveGenerator.isSquareAttacked(nextKingPos, activeColor, nextBoard);

        GameStatus nextStatus;
        GameResult nextResult = null;
        GameEndReason nextEndReason = null;

        if (opponentLegalMoves.isEmpty()) {
            if (inCheck) {
                nextStatus = GameStatus.CHECKMATE;
                nextResult = activeColor == PieceColor.WHITE ? GameResult.WHITE_WON : GameResult.BLACK_WON;
                nextEndReason = GameEndReason.CHECKMATE;
            } else {
                nextStatus = GameStatus.STALEMATE;
                nextResult = GameResult.DRAW;
                nextEndReason = GameEndReason.STALEMATE;
            }
        } else if (isInsufficientMaterial(nextBoard)) {
            nextStatus = GameStatus.DRAW;
            nextResult = GameResult.DRAW;
            nextEndReason = GameEndReason.INSUFFICIENT_MATERIAL;
        } else if (countRepetitions(nextHistory, posKey) >= 5) {
            nextStatus = GameStatus.DRAW;
            nextResult = GameResult.DRAW;
            nextEndReason = GameEndReason.THREEFOLD_REPETITION;
        } else if (nextHalfmove >= 150) {
            nextStatus = GameStatus.DRAW;
            nextResult = GameResult.DRAW;
            nextEndReason = GameEndReason.FIFTY_MOVE_RULE;
        } else if (inCheck) {
            nextStatus = GameStatus.CHECK;
        } else {
            nextStatus = GameStatus.ACTIVE;
        }

        return new GameState(
                nextBoard, nextColor, nextCastlingRights, nextEp,
                nextHalfmove, nextFullmove, nextStatus, nextResult, nextEndReason,
                nextHistory, nextMoveHistory
        );
    }

    public GameState resign(PieceColor player) {
        if (isEnded()) throw new IllegalStateException("이미 종료된 대국입니다.");
        GameResult res = player == PieceColor.WHITE ? GameResult.BLACK_WON : GameResult.WHITE_WON;
        return new GameState(
                board, activeColor, castlingRights, enPassantTarget,
                halfmoveClock, fullmoveNumber, GameStatus.RESIGNED, res, GameEndReason.RESIGNATION,
                positionHistory, moveHistory
        );
    }

    public GameState agreeDraw() {
        if (isEnded()) throw new IllegalStateException("이미 종료된 대국입니다.");
        return new GameState(
                board, activeColor, castlingRights, enPassantTarget,
                halfmoveClock, fullmoveNumber, GameStatus.DRAW, GameResult.DRAW, GameEndReason.AGREED_DRAW,
                positionHistory, moveHistory
        );
    }

    public GameState timeout(PieceColor timedOutPlayer) {
        if (isEnded()) throw new IllegalStateException("이미 종료된 대국입니다.");
        GameResult res = timedOutPlayer == PieceColor.WHITE ? GameResult.BLACK_WON : GameResult.WHITE_WON;
        return new GameState(
                board, activeColor, castlingRights, enPassantTarget,
                halfmoveClock, fullmoveNumber, GameStatus.TIMEOUT, res, GameEndReason.TIMEOUT,
                positionHistory, moveHistory
        );
    }

    public boolean canClaimThreefoldRepetition() {
        String currentKey = createPositionKey(board, activeColor, castlingRights, enPassantTarget);
        return countRepetitions(positionHistory, currentKey) >= 3;
    }

    public boolean canClaimFiftyMoveRule() {
        return halfmoveClock >= 100;
    }

    private static String createPositionKey(Board board, PieceColor color, CastlingRights castling, Position ep) {
        return board.toFen() + " " + (color == PieceColor.WHITE ? "w" : "b") + " " + castling.toFen() + " " + (ep != null ? ep.toAlgebraic() : "-");
    }

    private static int countRepetitions(List<String> history, String key) {
        int count = 0;
        for (String item : history) {
            if (item.equals(key)) count++;
        }
        return count;
    }

    public static boolean isInsufficientMaterial(Board board) {
        Map<Position, Piece> pieces = board.getAllPieces();
        if (pieces.size() > 4) return false;

        List<Piece> whitePieces = new ArrayList<>();
        List<Piece> blackPieces = new ArrayList<>();
        Position whiteBishopPos = null;
        Position blackBishopPos = null;

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Piece p = entry.getValue();
            if (p.type() == PieceType.PAWN || p.type() == PieceType.ROOK || p.type() == PieceType.QUEEN) return false;
            if (p.color() == PieceColor.WHITE) {
                whitePieces.add(p);
                if (p.type() == PieceType.BISHOP) whiteBishopPos = entry.getKey();
            } else {
                if (p.type() == PieceType.BISHOP) blackBishopPos = entry.getKey();
                blackPieces.add(p);
            }
        }

        if (whitePieces.size() == 1 && blackPieces.size() == 1) return true;

        if (whitePieces.size() == 2 && blackPieces.size() == 1) {
            Piece nonKing = whitePieces.get(0).type() == PieceType.KING ? whitePieces.get(1) : whitePieces.get(0);
            return nonKing.type() == PieceType.BISHOP || nonKing.type() == PieceType.KNIGHT;
        }
        if (whitePieces.size() == 1 && blackPieces.size() == 2) {
            Piece nonKing = blackPieces.get(0).type() == PieceType.KING ? blackPieces.get(1) : blackPieces.get(0);
            return nonKing.type() == PieceType.BISHOP || nonKing.type() == PieceType.KNIGHT;
        }

        if (whitePieces.size() == 2 && blackPieces.size() == 2 && whiteBishopPos != null && blackBishopPos != null) {
            boolean whiteLight = (whiteBishopPos.file() + whiteBishopPos.rank()) % 2 == 1;
            boolean blackLight = (blackBishopPos.file() + blackBishopPos.rank()) % 2 == 1;
            return whiteLight == blackLight;
        }

        return false;
    }

    public Board board() { return board; }
    public PieceColor activeColor() { return activeColor; }
    public CastlingRights castlingRights() { return castlingRights; }
    public Position enPassantTarget() { return enPassantTarget; }
    public int halfmoveClock() { return halfmoveClock; }
    public int fullmoveNumber() { return fullmoveNumber; }
    public GameStatus status() { return status; }
    public GameResult result() { return result; }
    public GameEndReason endReason() { return endReason; }
    public List<String> positionHistory() { return positionHistory; }
    public List<Move> moveHistory() { return moveHistory; }
}
