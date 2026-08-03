package com.freshlink.exception;

/**
 * Thrown when input is unacceptable in a way bean validation cannot express -
 * a password that fails a strength rule, an OTP that does not match. Maps to
 * HTTP 400: the caller sent something wrong and should change it before
 * retrying.
 */
public class ValidationException extends RuntimeException {

	public ValidationException(String message) {
		super(message);
	}
}
