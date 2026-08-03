package com.freshlink.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

/**
 * Token buckets held in Redis, so every instance draws on the same allowance.
 *
 * With per-process buckets, N replicas grant N times the configured limit -
 * five sign-in attempts becomes fifteen behind three replicas, which quietly
 * defeats the point of the limit.
 *
 * Selected by {@code app.ratelimit.backend=redis}; otherwise the in-memory
 * implementation is used and no Redis is required.
 */
@Service
@ConditionalOnProperty(name = "app.ratelimit.backend", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRateLimitService implements RateLimitService {

	private final ProxyManager<byte[]> proxyManager;

	@Override
	public boolean tryConsume(String key, int capacity, Duration window) {
		Supplier<BucketConfiguration> configuration = () -> BucketConfiguration.builder()
				.addLimit(Bandwidth.builder()
						.capacity(capacity)
						.refillIntervally(capacity, window)
						.build())
				.build();

		return proxyManager.builder()
				.build(key.getBytes(StandardCharsets.UTF_8), configuration)
				.tryConsume(1);
	}
}
