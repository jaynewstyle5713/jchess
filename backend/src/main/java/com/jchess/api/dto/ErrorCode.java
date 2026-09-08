package com.jchess.api.dto;

public enum ErrorCode {
    INVALID_REQUEST_PAYLOAD(400, false),
    GAME_NOT_FOUND(404, false),
    UNAUTHORIZED_PLAYER(403, false),
    NOT_YOUR_TURN(400, false),
    ILLEGAL_MOVE(400, false),
    IN_CHECK_MUST_DEFEND(400, false),
    PROMOTION_PIECE_REQUIRED(400, false),
    INVALID_PROMOTION_PIECE(400, false),
    GAME_VERSION_CONFLICT(409, true),
    GAME_ALREADY_FINISHED(400, false),
    DUPLICATE_REQUEST(400, false),
    OPPONENT_DISCONNECTED(200, true),
    INTERNAL_SERVER_ERROR(500, true);

    private final int httpStatus;
    private final boolean retryable;

    ErrorCode(int httpStatus, boolean retryable) {
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
