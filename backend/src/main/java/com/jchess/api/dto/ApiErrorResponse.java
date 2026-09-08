package com.jchess.api.dto;

import java.time.Instant;

public record ApiErrorResponse(
    String code,
    String message,
    String requestId,
    String gameId,
    Long gameVersion,
    boolean retryable,
    Instant timestamp
) {
    public static ApiErrorResponse of(String code, String message, String requestId, String gameId, Long gameVersion, boolean retryable) {
        return new ApiErrorResponse(code, message, requestId, gameId, gameVersion, retryable, Instant.now());
    }
}
