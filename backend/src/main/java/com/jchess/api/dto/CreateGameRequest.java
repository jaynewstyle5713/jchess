package com.jchess.api.dto;

public record CreateGameRequest(
        TimeControlDto timeControl,
        String preferredColor // "WHITE", "BLACK", "RANDOM"
) {
    public CreateGameRequest {
        if (timeControl == null) {
            timeControl = TimeControlDto.standard();
        }
        if (preferredColor == null || preferredColor.isBlank()) {
            preferredColor = "WHITE";
        }
    }
}
