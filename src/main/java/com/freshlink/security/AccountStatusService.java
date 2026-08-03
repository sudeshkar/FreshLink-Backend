package com.freshlink.security;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.freshlink.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Answers "may this account still be used?" for the JWT filter.
 *
 * The filter trusts the claims in a signed token, so a suspended or removed
 * account would otherwise keep working until that token expires. Checking the
 * database on every request would be correct but costly, so the answer is held
 * briefly - which makes the cache TTL, not the token lifetime, the bound on how
 * long a revoked account stays usable.
 */
@Service
@RequiredArgsConstructor
public class AccountStatusService {

	/** The revocation bound. Short on purpose. */
	public static final Duration TTL = Duration.ofSeconds(60);

	private final UserRepository userRepository;
	private final AccountStatusCache cache;

	public boolean isUsable(String email) {
		return cache.get(email).orElseGet(() -> {
			boolean usable = lookUp(email);
			cache.put(email, usable, TTL);
			return usable;
		});
	}

	private boolean lookUp(String email) {
		return userRepository.findByEmail(email)
				.map(user -> user.isActive() && user.getDeletedAt() == null)
				// An unknown email means the token names an account that no longer
				// exists, which is equally a reason to refuse it.
				.orElse(false);
	}
}
