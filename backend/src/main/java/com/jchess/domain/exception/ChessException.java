package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class ChessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String gameId;
    private final Long gameVersion;

    public ChessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public ChessException(ErrorCode errorCode, String message, String gameId, Long gameVersion) {
        super(message);
        this.errorCode = errorCode;
        this.gameId = gameId;
        this.gameVersion = gameVersion;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getGameId() {
        return gameId;
    }

    public Long getGameVersion() {
        return gameVersion;
    }
}
