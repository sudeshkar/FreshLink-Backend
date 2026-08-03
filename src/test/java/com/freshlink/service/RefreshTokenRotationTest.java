package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.freshlink.Repository.RefreshTokenRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.authdto.AuthResponseDto;
import com.freshlink.enums.Role;
import com.freshlink.exception.InvalidRefreshTokenException;
import com.freshlink.model.Cafe;
import com.freshlink.model.RefreshToken;
import com.freshlink.service.interfaces.impl.RefreshTokenServiceImpl;
import com.freshlink.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRotationTest {

	@Mock private RefreshTokenRepository repo;
	@Mock private JwtUtil jwtUtil;
	@Mock private UserRepository userRepository;

	@InjectMocks private RefreshTokenServiceImpl service;

	private static final String EMAIL = "cafe@freshlink.test";

	@BeforeEach
	void setExpiry() {
		ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);
	}

	private Cafe activeCafe() {
		Cafe cafe = new Cafe();
		cafe.setId(1L);
		cafe.setEmail(EMAIL);
		cafe.setRole(Role.CAFE);
		cafe.setActive(true);
		return cafe;
	}

	/** Issues a token through the service so the stored hash matches the raw value. */
	private String issueToken(ArgumentCaptor<RefreshToken> captor) {
		when(repo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
		String raw = service.createToken(EMAIL);
		verify(repo).save(captor.capture());
		return raw;
	}

	@Test
	@DisplayName("the raw token is never persisted, only its hash")
	void storesOnlyTheHash() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		String raw = issueToken(captor);

		RefreshToken stored = captor.getValue();
		assertThat(stored.getTokenHash())
				.as("a database dump must not yield a usable token")
				.isNotEqualTo(raw);
		assertThat(stored.getEmail()).isEqualTo(EMAIL);
	}

	@Test
	@DisplayName("rotating marks the old token used and returns a different one")
	void rotationInvalidatesThePresentedToken() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		String raw = issueToken(captor);
		RefreshToken stored = captor.getValue();
		stored.setExpiryTime(LocalDateTime.now().plusDays(7));

		when(repo.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeCafe()));
		when(jwtUtil.generateToken(anyString(), any())).thenReturn("new-access-token");

		AuthResponseDto response = service.rotate(raw);

		assertThat(stored.isUsed()).isTrue();
		assertThat(response.refreshToken()).isNotEqualTo(raw);
		assertThat(response.accessToken()).isEqualTo("new-access-token");
	}

	@Test
	@DisplayName("replaying a used token revokes every session for that account")
	void replayRevokesTheWholeFamily() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		String raw = issueToken(captor);
		RefreshToken stored = captor.getValue();
		stored.setExpiryTime(LocalDateTime.now().plusDays(7));
		stored.setUsed(true);

		when(repo.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> service.rotate(raw))
				.isInstanceOf(InvalidRefreshTokenException.class)
				.hasMessageContaining("already been used");

		// The honest holder cannot be told apart from the attacker, so both lose access.
		verify(repo).deleteByEmail(EMAIL);
	}

	@Test
	@DisplayName("an expired token is refused without revoking anything")
	void expiredTokenIsRefused() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		String raw = issueToken(captor);
		RefreshToken stored = captor.getValue();
		stored.setExpiryTime(LocalDateTime.now().minusMinutes(1));

		when(repo.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> service.rotate(raw))
				.isInstanceOf(InvalidRefreshTokenException.class)
				.hasMessageContaining("expired");

		verify(repo, never()).deleteByEmail(anyString());
	}

	@Test
	@DisplayName("a suspended account cannot refresh its way back in")
	void suspendedAccountCannotRefresh() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		String raw = issueToken(captor);
		RefreshToken stored = captor.getValue();
		stored.setExpiryTime(LocalDateTime.now().plusDays(7));

		Cafe suspended = activeCafe();
		suspended.setActive(false);

		when(repo.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(suspended));

		assertThatThrownBy(() -> service.rotate(raw))
				.isInstanceOf(InvalidRefreshTokenException.class)
				.hasMessageContaining("no longer active");

		verify(repo).deleteByEmail(EMAIL);
	}

	@Test
	@DisplayName("a soft-deleted account cannot refresh either")
	void softDeletedAccountCannotRefresh() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		String raw = issueToken(captor);
		RefreshToken stored = captor.getValue();
		stored.setExpiryTime(LocalDateTime.now().plusDays(7));

		Cafe removed = activeCafe();
		removed.setDeletedAt(LocalDateTime.now());

		when(repo.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(removed));

		assertThatThrownBy(() -> service.rotate(raw))
				.isInstanceOf(InvalidRefreshTokenException.class);

		verify(repo).deleteByEmail(EMAIL);
	}

	@Test
	@DisplayName("an unknown token is refused")
	void unknownTokenIsRefused() {
		when(repo.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.rotate("not-a-real-token"))
				.isInstanceOf(InvalidRefreshTokenException.class)
				.hasMessageContaining("Invalid refresh token");
	}
}
