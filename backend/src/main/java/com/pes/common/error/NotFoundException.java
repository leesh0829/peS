package com.pes.common.error;

public class NotFoundException extends BusinessException {

	public NotFoundException(String message) {
		super("RESOURCE_NOT_FOUND", message);
	}
}
