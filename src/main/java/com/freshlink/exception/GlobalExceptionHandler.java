package com.freshlink.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, WebRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
		return build(HttpStatus.CONFLICT,
				"This item was updated by someone else. Please retry.", request);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex, WebRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex, WebRequest request) {
		return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
		return build(HttpStatus.FORBIDDEN, "Access denied", request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, WebRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

		ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validation failed", path(request), fieldErrors);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiError> handleValidationRule(ValidationException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(AccountNotActiveException.class)
	public ResponseEntity<ApiError> handleAccountNotActive(AccountNotActiveException ex, WebRequest request) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	/** Malformed JSON, or a body that cannot be bound to the request record. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request);
	}

	/** A path variable or query parameter of the wrong type, such as /orders/abc. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST,
				"'%s' is not a valid value for %s".formatted(ex.getValue(), ex.getName()), request);
	}

	/**
	 * Anything reaching here is unexpected, not a domain outcome: every business
	 * failure has a typed exception above. There used to be a RuntimeException
	 * handler returning 400, which meant a genuine null pointer was reported to
	 * the client as though they had sent a bad request.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unhandled exception at {}", path(request), ex);
		// Deliberately generic: internal details stay in the logs, not the response.
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request) {
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), status.getReasonPhrase(), message, path(request)));
	}

	private String path(WebRequest request) {
		return request.getDescription(false).replaceFirst("^uri=", "");
	}
}
