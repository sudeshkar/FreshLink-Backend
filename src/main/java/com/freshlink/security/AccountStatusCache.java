package com.freshlink.security;

import java.time.Duration;
import java.util.Optional;

/**
 * Remembers whether an account may still be used.
 *
 * Deliberately its own small abstraction rather than Spring's cache
 * annotations: this is what bounds how long a revoked account keeps working, so
 * it needs to be obviously correct and directly testable, not dependent on
 * which manager an interceptor happens to resolve.
 */
public interface AccountStatusCache {

	Optional<Boolean> get(String email);

	void put(String email, boolean usable, Duration ttl);

	void evict(String email);
}
