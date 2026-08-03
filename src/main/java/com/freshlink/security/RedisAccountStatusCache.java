package com.freshlink.security;

import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Shared across instances, so every replica agrees about an account and a
 * revocation is seen everywhere within the TTL.
 *
 * Uses StringRedisTemplate with an explicit expiry rather than Spring's cache
 * abstraction: a plain string and a SETEX is trivially inspectable, and the
 * expiry is the security-relevant part.
 */
@Component
@ConditionalOnProperty(name = "app.cache.backend", havingValue = "redis")
@RequiredArgsConstructor
public class RedisAccountStatusCache implements AccountStatusCache {

	private static final String PREFIX = "account-status::";

	private final StringRedisTemplate redis;

	@Override
	public Optional<Boolean> get(String email) {
		String value = redis.opsForValue().get(PREFIX + email);
		return value == null ? Optional.empty() : Optional.of(Boolean.parseBoolean(value));
	}

	@Override
	public void put(String email, boolean usable, Duration ttl) {
		redis.opsForValue().set(PREFIX + email, Boolean.toString(usable), ttl);
	}

	@Override
	public void evict(String email) {
		redis.delete(PREFIX + email);
	}
}
