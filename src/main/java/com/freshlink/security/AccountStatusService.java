package com.freshlink.security;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.freshlink.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Answers "may this account still be used?" for the JWT filter.
 *
 * The filter trusts the claims in a signed token, which means a suspended or
 * removed account keeps working until its access token expires. Checking the
 * database on every request would be correct but costly, so the answer is
 * cached and the cache is swept once a minute - bounding how long a revoked
 * account stays usable to about a minute rather than a full token lifetime.
 */
@Service
@RequiredArgsConstructor
public class AccountStatusService {

	public static final String CACHE = "account-status";

	private final UserRepository userRepository;

	@Cacheable(CACHE)
	public boolean isUsable(String email) {
		return userRepository.findByEmail(email)
				.map(user -> user.isActive() && user.getDeletedAt() == null)
				// An unknown email means the token names an account that no longer
				// exists, which is equally a reason to refuse it.
				.orElse(false);
	}
}
