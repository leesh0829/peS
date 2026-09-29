package com.pes.common.error;

public class ConflictException extends BusinessException {

	public ConflictException(String message) {
		super("RESOURCE_CONFLICT", message);
	}
}
