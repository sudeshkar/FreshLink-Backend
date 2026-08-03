package com.freshlink.exception;

/**
 * Thrown when a refresh token is unknown, expired, already used, or belongs to
 * an account that can no longer sign in. Maps to HTTP 401 so clients treat it
 * as "session over, sign in again" rather than a retryable error.
 */
public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException(String message) {
		super(message);
	}
}
