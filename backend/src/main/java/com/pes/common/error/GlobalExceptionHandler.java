package com.pes.common.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pes.common.api.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
		return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getCode(), exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(fieldError ->
				fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

		return ResponseEntity.badRequest().body(new ApiErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"VALIDATION_FAILED",
				"입력값을 확인해 주세요.",
				fieldErrors));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
		return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "이미 사용 중인 값이거나 연결된 데이터가 있습니다.");
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ApiErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
		return error(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "다른 사용자가 먼저 변경했습니다. 새로고침 후 다시 시도해 주세요.");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
		log.error("Unhandled server exception", exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), code, message));
	}
}
