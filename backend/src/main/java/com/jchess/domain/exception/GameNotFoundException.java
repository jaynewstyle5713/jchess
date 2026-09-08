package com.jchess.domain.exception;

import com.jchess.api.dto.ErrorCode;

public class GameNotFoundException extends ChessException {
    public GameNotFoundException(String gameId) {
        super(ErrorCode.GAME_NOT_FOUND, "대국을 찾을 수 없습니다: " + gameId, gameId, null);
    }
}
