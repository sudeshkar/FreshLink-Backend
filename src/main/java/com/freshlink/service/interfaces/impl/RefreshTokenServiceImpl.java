package com.freshlink.service.interfaces.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.RefreshTokenRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.authdto.AuthResponseDto;
import com.freshlink.exception.InvalidRefreshTokenException;
import com.freshlink.model.RefreshToken;
import com.freshlink.model.User;
import com.freshlink.service.interfaces.RefreshTokenService;
import com.freshlink.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository repo;
	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	@Value("${jwt.refresh-expiration-days:7}")
	private long refreshExpirationDays;

	@Override
	public String createToken(String email) {
		byte[] raw = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(raw);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

		RefreshToken token = new RefreshToken();
		token.setEmail(email);
		token.setTokenHash(hash(rawToken));
		token.setExpiryTime(LocalDateTime.now().plusDays(refreshExpirationDays));
		repo.save(token);

		// The caller receives the only readable copy.
		return rawToken;
	}

	/**
	 * The revocation paths below delete rows and then throw. Without
	 * {@code noRollbackFor} the throw would roll that delete straight back, so the
	 * caller would see a 401 while every session it was meant to kill stayed live
	 * - the failure mode looks fixed from the outside and is not.
	 */
	@Override
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public AuthResponseDto rotate(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new InvalidRefreshTokenException("Refresh token is required");
		}

		RefreshToken stored = repo.findByTokenHash(hash(rawRefreshToken))
				.orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

		if (stored.isUsed()) {
			// The rightful holder already exchanged this token, so this presentation
			// is a replay: either the token leaked or the account is compromised.
			// There is no way to tell which party is genuine, so end every session.
			log.warn("Refresh token replay detected for {} - revoking all sessions", stored.getEmail());
			repo.deleteByEmail(stored.getEmail());
			throw new InvalidRefreshTokenException(
					"This refresh token has already been used. Please sign in again.");
		}

		if (stored.getExpiryTime().isBefore(LocalDateTime.now())) {
			throw new InvalidRefreshTokenException("Refresh token expired");
		}

		User user = userRepository.findByEmail(stored.getEmail())
				.orElseThrow(() -> new InvalidRefreshTokenException("Account no longer exists"));

		// Without this, a suspended or removed account could refresh its way back in
		// indefinitely - the access token check at login would never be reached again.
		if (!user.isActive() || user.getDeletedAt() != null) {
			repo.deleteByEmail(user.getEmail());
			throw new InvalidRefreshTokenException("Account is no longer active");
		}

		stored.setUsed(true);
		repo.save(stored);

		return new AuthResponseDto(
				jwtUtil.generateToken(user.getEmail(), user.getRole()),
				createToken(user.getEmail()),
				user.getRole().name());
	}

	@Override
	public void deleteByEmail(String email) {
		repo.deleteByEmail(email);
	}

	/**
	 * Used tokens are retained past exchange so replay stays detectable, so
	 * something has to clear them out once they can no longer be presented.
	 */
	@Scheduled(cron = "${app.refresh-token.purge-cron:0 0 3 * * *}")
	public void purgeExpiredTokens() {
		repo.deleteByExpiryTimeBefore(LocalDateTime.now());
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return Base64.getEncoder().encodeToString(
					digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 is required of every JVM; if it is missing the platform is broken.
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}
