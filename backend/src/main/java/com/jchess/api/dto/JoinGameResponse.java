package com.jchess.api.dto;

import com.jchess.domain.model.GameStatus;

public record JoinGameResponse(
        String gameId,
        GameStatus status,
        PlayerInfoDto whitePlayer,
        PlayerInfoDto blackPlayer,
        Long gameVersion
) {}
