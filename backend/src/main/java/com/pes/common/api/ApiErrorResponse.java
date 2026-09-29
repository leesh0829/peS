package com.pes.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		Map<String, String> fieldErrors) {

	public static ApiErrorResponse of(int status, String code, String message) {
		return new ApiErrorResponse(Instant.now(), status, code, message, Map.of());
	}
}
