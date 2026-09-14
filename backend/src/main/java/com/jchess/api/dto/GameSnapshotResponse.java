package com.jchess.api.dto;

import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameResult;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;

import java.time.Instant;

public record GameSnapshotResponse(
        String gameId,
        GameMode gameMode,
        Integer aiLevel,
        int remainingHints,
        int maxUndos,
        int remainingUndos,
        GameStatus gameStatus,
        Long gameVersion,
        PieceColor turn,
        String fen,
        PlayerInfoDto whitePlayer,
        PlayerInfoDto blackPlayer,
        MoveDto lastMove,
        boolean isCheck,
        GameResult result,
        GameEndReason endReason,
        Instant createdAt,
        Instant updatedAt
) {
    public GameSnapshotResponse(
            String gameId,
            GameMode gameMode,
            Integer aiLevel,
            GameStatus gameStatus,
            Long gameVersion,
            PieceColor turn,
            String fen,
            PlayerInfoDto whitePlayer,
            PlayerInfoDto blackPlayer,
            MoveDto lastMove,
            boolean isCheck,
            GameResult result,
            GameEndReason endReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(gameId, gameMode, aiLevel, 3, 3, 3, gameStatus, gameVersion, turn, fen,
                whitePlayer, blackPlayer, lastMove, isCheck, result, endReason, createdAt, updatedAt);
    }
}


