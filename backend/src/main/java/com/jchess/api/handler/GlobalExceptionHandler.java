package com.jchess.api.handler;

import com.jchess.api.dto.ApiErrorResponse;
import com.jchess.api.dto.ErrorCode;
import com.jchess.domain.exception.ChessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChessException.class)
    public ResponseEntity<ApiErrorResponse> handleChessException(ChessException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = HttpStatus.valueOf(code.getHttpStatus());
        ApiErrorResponse response = ApiErrorResponse.of(
                code.name(),
                ex.getMessage(),
                null,
                ex.getGameId(),
                ex.getGameVersion(),
                code.isRetryable()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        ErrorCode code = ErrorCode.GAME_VERSION_CONFLICT;
        ApiErrorResponse response = ApiErrorResponse.of(
                code.name(),
                "동시 변경 충돌이 발생했습니다. 최신 대국 상태를 동기화한 후 다시 시도하십시오.",
                null,
                null,
                null,
                true
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(WebExchangeBindException ex) {
        ErrorCode code = ErrorCode.INVALID_REQUEST_PAYLOAD;
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getField() + ": " + ex.getBindingResult().getFieldError().getDefaultMessage()
                : "요청 본문 검증에 실패했습니다.";

        ApiErrorResponse response = ApiErrorResponse.of(
                code.name(),
                message,
                null,
                null,
                null,
                false
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorCode code = ErrorCode.INVALID_REQUEST_PAYLOAD;
        ApiErrorResponse response = ApiErrorResponse.of(
                code.name(),
                ex.getMessage(),
                null,
                null,
                null,
                false
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorCode code = ErrorCode.GAME_ALREADY_FINISHED;
        ApiErrorResponse response = ApiErrorResponse.of(
                code.name(),
                ex.getMessage(),
                null,
                null,
                null,
                false
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex) {
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        ApiErrorResponse response = ApiErrorResponse.of(
                code.name(),
                "서버 내부 처리 중 오류가 발생했습니다.",
                null,
                null,
                null,
                true
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
