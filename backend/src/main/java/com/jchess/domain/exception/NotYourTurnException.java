package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class NotYourTurnException extends ChessException {
    public NotYourTurnException(String message, String gameId, Long gameVersion) {
        super(ErrorCode.NOT_YOUR_TURN, message, gameId, gameVersion);
    }
}
