package com.freshlink.service.interfaces;

import com.freshlink.authdto.AuthResponseDto;

public interface RefreshTokenService {

	/**
	 * Issues a refresh token for {@code email} and returns the raw value. Only the
	 * hash is persisted, so this is the one and only time the token is readable.
	 */
	String createToken(String email);

	/**
	 * Exchanges a refresh token for a fresh access token and a replacement refresh
	 * token, invalidating the one presented.
	 */
	AuthResponseDto rotate(String rawRefreshToken);

	void deleteByEmail(String email);
}
