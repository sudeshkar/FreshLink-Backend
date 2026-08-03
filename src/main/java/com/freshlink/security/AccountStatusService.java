package com.freshlink.security;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.freshlink.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Answers "may this account still be used?" for the JWT filter.
 *
 * The filter trusts the claims in a signed token, which means a suspended or
 * removed account keeps working until its access token expires. Checking the
 * database on every request would be correct but costly, so the answer is
 * cached - bounding how long a revoked account stays usable to the cache
 * lifetime rather than the token lifetime.
 *
 * The cache is read and written explicitly rather than through {@code @Cacheable}.
 * That annotation depends on proxying and on which manager the interceptor
 * resolves, and it was silently not populating the Redis-backed cache - which
 * would have meant a database hit per request and a revocation bound that only
 * held for the in-process case. Doing it here is a few more lines and leaves
 * nothing to infer.
 */
@Service
@RequiredArgsConstructor
public class AccountStatusService {

	public static final String CACHE = "account-status";

	private final UserRepository userRepository;
	private final CacheManager cacheManager;

	public boolean isUsable(String email) {
		Cache cache = cacheManager.getCache(CACHE);

		if (cache != null) {
			Cache.ValueWrapper cached = cache.get(email);
			if (cached != null) {
				return Boolean.TRUE.equals(cached.get());
			}
		}

		boolean usable = lookUp(email);

		if (cache != null) {
			cache.put(email, usable);
		}
		return usable;
	}

	private boolean lookUp(String email) {
		return userRepository.findByEmail(email)
				.map(user -> user.isActive() && user.getDeletedAt() == null)
				// An unknown email means the token names an account that no longer
				// exists, which is equally a reason to refuse it.
				.orElse(false);
	}
}
