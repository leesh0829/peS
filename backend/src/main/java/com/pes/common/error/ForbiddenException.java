package com.pes.common.error;

public class ForbiddenException extends BusinessException {

	public ForbiddenException(String message) {
		super("ACCESS_DENIED", message);
	}
}
