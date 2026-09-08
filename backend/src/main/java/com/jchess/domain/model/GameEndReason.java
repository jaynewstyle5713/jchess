package com.jchess.domain.model;

public enum GameEndReason {
    CHECKMATE,
    STALEMATE,
    RESIGNATION,
    TIMEOUT,
    AGREED_DRAW,
    INSUFFICIENT_MATERIAL,
    THREEFOLD_REPETITION,
    FIFTY_MOVE_RULE
}
