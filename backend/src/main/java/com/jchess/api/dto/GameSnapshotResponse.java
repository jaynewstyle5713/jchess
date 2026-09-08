package com.jchess.api.dto;

import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameResult;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;

import java.time.Instant;

public record GameSnapshotResponse(
        String gameId,
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
) {}
