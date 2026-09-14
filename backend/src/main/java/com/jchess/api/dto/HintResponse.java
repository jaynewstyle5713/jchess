package com.jchess.api.dto;

public record HintResponse(
        String from,
        String to,
        String promotion,
        int remainingHints,
        String message
) {}
