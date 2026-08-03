package com.freshlink.security;

import java.time.Duration;

/**
 * Token-bucket allowances keyed by caller identity.
 *
 * Two implementations: in-memory by default, and Redis-backed when
 * {@code app.ratelimit.backend=redis}. The distinction matters as soon as there
 * is more than one instance - per-process buckets mean each replica grants the
 * full allowance, so N replicas multiply every limit by N.
 */
public interface RateLimitService {

	/**
	 * Consumes one token for {@code key}, returning false when the allowance is
	 * spent. {@code capacity} tokens are granted, refilled together every
	 * {@code window}.
	 */
	boolean tryConsume(String key, int capacity, Duration window);
}
