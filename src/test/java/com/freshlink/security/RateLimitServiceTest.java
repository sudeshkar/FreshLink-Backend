package com.freshlink.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

	private final InMemoryRateLimitService rateLimitService = new InMemoryRateLimitService();

	@Test
	@DisplayName("allows exactly the configured capacity, then refuses")
	void allowsUpToCapacity() {
		Duration window = Duration.ofMinutes(15);

		for (int attempt = 1; attempt <= 5; attempt++) {
			assertThat(rateLimitService.tryConsume("login|1.2.3.4", 5, window))
					.as("attempt %d should be allowed", attempt)
					.isTrue();
		}

		assertThat(rateLimitService.tryConsume("login|1.2.3.4", 5, window))
				.as("sixth attempt should be refused")
				.isFalse();
	}

	@Test
	@DisplayName("allowances are independent per key, so one caller cannot lock out another")
	void keysAreIndependent() {
		Duration window = Duration.ofMinutes(15);

		for (int i = 0; i < 5; i++) {
			rateLimitService.tryConsume("login|1.2.3.4", 5, window);
		}

		assertThat(rateLimitService.tryConsume("login|1.2.3.4", 5, window)).isFalse();
		assertThat(rateLimitService.tryConsume("login|5.6.7.8", 5, window)).isTrue();
	}

	@Test
	@DisplayName("tokens come back after the window elapses")
	void refillsAfterWindow() throws InterruptedException {
		Duration window = Duration.ofMillis(300);

		assertThat(rateLimitService.tryConsume("short|key", 1, window)).isTrue();
		assertThat(rateLimitService.tryConsume("short|key", 1, window)).isFalse();

		Thread.sleep(400);

		assertThat(rateLimitService.tryConsume("short|key", 1, window))
				.as("allowance should be restored once the window passes")
				.isTrue();
	}

	@Test
	@DisplayName("idle buckets are evicted, so the map does not grow per-IP forever")
	void evictsIdleBuckets() throws InterruptedException {
		Duration window = Duration.ofMillis(200);

		rateLimitService.tryConsume("idle|a", 2, window);
		rateLimitService.tryConsume("busy|b", 2, window);
		rateLimitService.tryConsume("busy|b", 2, window);
		assertThat(rateLimitService.trackedKeys()).isEqualTo(2);

		// Let both refill, at which point neither holds state worth keeping.
		Thread.sleep(300);
		rateLimitService.evictIdleBuckets();

		assertThat(rateLimitService.trackedKeys()).isZero();
	}

	@Test
	@DisplayName("a partially spent bucket survives eviction")
	void keepsActiveBuckets() {
		rateLimitService.tryConsume("active|key", 5, Duration.ofMinutes(15));

		rateLimitService.evictIdleBuckets();

		assertThat(rateLimitService.trackedKeys())
				.as("a bucket mid-window still carries the caller's spent allowance")
				.isEqualTo(1);
	}
}
