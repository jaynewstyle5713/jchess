package com.jchess.domain.model;

public enum GameStatus {
    WAITING_FOR_OPPONENT,
    ACTIVE,
    CHECK,
    CHECKMATE,
    STALEMATE,
    DRAW,
    RESIGNED,
    TIMEOUT
}
