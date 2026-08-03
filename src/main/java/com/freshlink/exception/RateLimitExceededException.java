package com.freshlink.exception;

/** Thrown when a caller exceeds an allowance. Maps to HTTP 429. */
public class RateLimitExceededException extends RuntimeException {

	public RateLimitExceededException(String message) {
		super(message);
	}
}
