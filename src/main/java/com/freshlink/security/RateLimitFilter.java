package com.freshlink.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Caps requests to the unauthenticated auth endpoints by client address.
 *
 * Login and OTP issuance are the only routes reachable without a token, which
 * makes them the natural targets for credential stuffing and for burning the
 * SMTP quota. Everything else sits behind {@link JwtFilter}.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitService rateLimitService;

	@Value("${app.ratelimit.login.capacity:5}")
	private int loginCapacity;

	@Value("${app.ratelimit.login.window-minutes:15}")
	private int loginWindowMinutes;

	@Value("${app.ratelimit.otp.capacity:3}")
	private int otpCapacity;

	@Value("${app.ratelimit.otp.window-minutes:15}")
	private int otpWindowMinutes;

	@Value("${app.ratelimit.enabled:true}")
	private boolean enabled;

	/** Path prefix to the message shown when its allowance runs out. */
	private static final Map<String, String> LIMITED_PATHS = Map.of(
			"/api/v1/auth/login", "Too many sign-in attempts. Please try again later.",
			"/api/v1/auth/request-otp", "Too many verification codes requested. Please try again later.",
			"/api/v1/auth/verify-otp", "Too many verification attempts. Please try again later.");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String path = request.getRequestURI();
		String message = LIMITED_PATHS.get(path);

		if (!enabled || message == null) {
			filterChain.doFilter(request, response);
			return;
		}

		boolean isLogin = path.endsWith("/login");
		int capacity = isLogin ? loginCapacity : otpCapacity;
		Duration window = Duration.ofMinutes(isLogin ? loginWindowMinutes : otpWindowMinutes);

		if (!rateLimitService.tryConsume(path + "|" + clientIp(request), capacity, window)) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write(
					"{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"%s\"}".formatted(message));
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Reads X-Forwarded-For when present. Only trustworthy behind a proxy that
	 * overwrites the header — exposed directly, a client can forge it and sidestep
	 * the limit, so terminate TLS behind a proxy you control.
	 */
	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
