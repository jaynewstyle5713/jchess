package com.jchess.api.dto;

import com.jchess.domain.model.PieceColor;

public record GameStateUpdatedPayload(
        int moveNumber,
        PieceColor turn,
        MoveDto lastMove,
        String fen,
        boolean isCheck,
        long whiteRemainingTimeMs,
        long blackRemainingTimeMs
) {}
