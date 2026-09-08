package com.jchess.api.dto;

public record PlayerInfoDto(
        String id,
        String name,
        long remainingTimeMs,
        boolean isOnline
) {}
