package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class GameVersionConflictException extends ChessException {
    public GameVersionConflictException(String message, String gameId, Long gameVersion) {
        super(ErrorCode.GAME_VERSION_CONFLICT, message, gameId, gameVersion);
    }
}
