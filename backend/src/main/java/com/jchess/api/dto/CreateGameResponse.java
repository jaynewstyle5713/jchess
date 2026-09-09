package com.jchess.api.dto;

import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameStatus;

import java.time.Instant;

public record CreateGameResponse(
        String gameId,
        GameMode gameMode,
        Integer aiLevel,
        GameStatus status,
        PlayerInfoDto whitePlayer,
        PlayerInfoDto blackPlayer,
        Long gameVersion,
        Instant createdAt
) {}

