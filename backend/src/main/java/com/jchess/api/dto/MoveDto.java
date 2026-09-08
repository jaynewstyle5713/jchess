package com.jchess.api.dto;

public record MoveDto(
        String from,
        String to,
        String piece,
        String captured,
        String promotion,
        String notation
) {}
