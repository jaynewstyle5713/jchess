package com.jchess.api.dto;

public record ErrorPayload(
        String code,
        String message,
        String requestId,
        boolean retryable
) {}
