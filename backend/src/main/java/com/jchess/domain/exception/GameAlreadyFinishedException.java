package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class GameAlreadyFinishedException extends ChessException {
    public GameAlreadyFinishedException(String message, String gameId, Long gameVersion) {
        super(ErrorCode.GAME_ALREADY_FINISHED, message, gameId, gameVersion);
    }
}
