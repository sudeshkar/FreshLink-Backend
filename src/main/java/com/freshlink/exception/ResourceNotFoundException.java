package com.freshlink.exception;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String entity, Object id) {
		super("%s not found: %s".formatted(entity, id));
	}
}
