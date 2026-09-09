package com.jchess.api.dto;

import com.jchess.domain.model.GameMode;

public record CreateGameRequest(
        GameMode gameMode,
        Integer aiLevel,
        TimeControlDto timeControl,
        String preferredColor // "WHITE", "BLACK", "RANDOM"
) {
    public CreateGameRequest {
        if (gameMode == null) {
            gameMode = GameMode.PVP;
        }
        if (aiLevel == null || aiLevel <= 0) {
            aiLevel = 2000;
        }
        if (timeControl == null) {
            timeControl = TimeControlDto.standard();
        }
        if (preferredColor == null || preferredColor.isBlank()) {
            preferredColor = "WHITE";
        }
    }

    public CreateGameRequest(TimeControlDto timeControl, String preferredColor) {
        this(GameMode.PVP, 2000, timeControl, preferredColor);
    }
}

