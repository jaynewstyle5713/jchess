package com.jchess.api.dto;

public record MoveRejectedPayload(
        String requestId,
        String errorCode,
        String message,
        Long expectedGameVersion
) {}
