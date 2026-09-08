package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class UnauthorizedPlayerException extends ChessException {
    public UnauthorizedPlayerException(String message, String gameId) {
        super(ErrorCode.UNAUTHORIZED_PLAYER, message, gameId, null);
    }
}
