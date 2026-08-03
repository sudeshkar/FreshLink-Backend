package com.freshlink.exception;

/**
 * Thrown when credentials are correct but the account may not sign in yet -
 * awaiting email verification or admin approval. Maps to HTTP 403 rather than
 * 401: retrying with different credentials will not help, so the client should
 * show "pending approval" rather than "wrong password".
 */
public class AccountNotActiveException extends RuntimeException {

	public AccountNotActiveException(String message) {
		super(message);
	}
}
