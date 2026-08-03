package com.freshlink.exception;

/**
 * Thrown when a request is well-formed but violates a domain rule - ordering
 * more stock than is available, rating an order that is not complete, and so
 * on. Maps to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
