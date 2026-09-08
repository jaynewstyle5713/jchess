package com.jchess.api.dto;

import com.jchess.domain.model.GameStatus;

import java.time.Instant;

public record CreateGameResponse(
        String gameId,
        GameStatus status,
        PlayerInfoDto whitePlayer,
        PlayerInfoDto blackPlayer,
        Long gameVersion,
        Instant createdAt
) {}
