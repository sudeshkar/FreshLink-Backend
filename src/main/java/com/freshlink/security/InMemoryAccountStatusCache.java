package com.freshlink.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Per-process, so each replica keeps its own view: one can still accept a
 * suspended account after another has noticed. Fine for a single instance, and
 * the default so that running locally needs no Redis.
 */
@Component
@ConditionalOnProperty(name = "app.cache.backend", havingValue = "simple", matchIfMissing = true)
public class InMemoryAccountStatusCache implements AccountStatusCache {

	private record Entry(boolean usable, Instant expiresAt) {
	}

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();

	@Override
	public Optional<Boolean> get(String email) {
		Entry entry = entries.get(email);
		if (entry == null) {
			return Optional.empty();
		}
		// Expiry is checked on read, so a stale entry can never be served even if
		// nothing has swept the map yet.
		if (entry.expiresAt().isBefore(Instant.now())) {
			entries.remove(email);
			return Optional.empty();
		}
		return Optional.of(entry.usable());
	}

	@Override
	public void put(String email, boolean usable, Duration ttl) {
		entries.put(email, new Entry(usable, Instant.now().plus(ttl)));
	}

	@Override
	public void evict(String email) {
		entries.remove(email);
	}
}
