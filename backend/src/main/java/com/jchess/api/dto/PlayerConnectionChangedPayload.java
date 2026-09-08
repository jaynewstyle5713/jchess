package com.jchess.api.dto;

public record PlayerConnectionChangedPayload(
        String playerId,
        boolean isOnline
) {}
