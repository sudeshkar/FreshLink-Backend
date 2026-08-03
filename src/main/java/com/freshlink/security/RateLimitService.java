package com.freshlink.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Token-bucket allowances keyed by caller identity.
 *
 * Buckets live in memory, so limits are per-instance: running two replicas
 * doubles the effective allowance. That is an acceptable trade for a single
 * deployment, but this needs to move to a shared store (bucket4j-redis)
 * before scaling horizontally.
 */
@Service
public class RateLimitService {

	/** Keeps the configured capacity alongside the bucket, so idle ones can be spotted. */
	private record Allowance(Bucket bucket, long capacity) {
		boolean isIdle() {
			// Fully refilled means nothing has been consumed this window.
			return bucket.getAvailableTokens() >= capacity;
		}
	}

	private final Map<String, Allowance> allowances = new ConcurrentHashMap<>();

	/**
	 * Consumes one token for {@code key}, returning false when the allowance is
	 * spent. {@code capacity} tokens are granted, refilled together every
	 * {@code window}.
	 */
	public boolean tryConsume(String key, int capacity, Duration window) {
		Allowance allowance = allowances.computeIfAbsent(key, ignored -> new Allowance(
				Bucket.builder()
						.addLimit(Bandwidth.builder()
								.capacity(capacity)
								.refillIntervally(capacity, window)
								.build())
						.build(),
				capacity));

		return allowance.bucket().tryConsume(1);
	}

	/**
	 * Drops buckets that have refilled to capacity. Without this the map gains an
	 * entry per distinct IP and never releases one, which is a slow memory leak on
	 * a public endpoint. A fully refilled bucket carries no state worth keeping —
	 * recreating it costs nothing and yields the same allowance.
	 */
	@Scheduled(fixedRate = 600_000)
	public void evictIdleBuckets() {
		allowances.entrySet().removeIf(entry -> entry.getValue().isIdle());
	}

	/** Visible for tests. */
	int trackedKeys() {
		return allowances.size();
	}
}
