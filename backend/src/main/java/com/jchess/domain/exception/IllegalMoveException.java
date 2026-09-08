package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class IllegalMoveException extends ChessException {
    public IllegalMoveException(String message, String gameId, Long gameVersion) {
        super(ErrorCode.ILLEGAL_MOVE, message, gameId, gameVersion);
    }
}
