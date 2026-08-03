package com.freshlink.exception;

import java.time.Instant;
import java.util.Map;

/** Uniform error body returned for every failed request. */
public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> fieldErrors) {

	public static ApiError of(int status, String error, String message, String path) {
		return new ApiError(Instant.now(), status, error, message, path, null);
	}
}
