package com.freshlink.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tags every request with an id so its log lines can be found together.
 *
 * Without one, a report of "my order failed" means grepping by timestamp and
 * hoping - and the notification listeners run on a background pool, so their
 * lines are not even adjacent to the request that triggered them.
 *
 * An inbound X-Request-Id is honoured, so a trace started by a gateway or the
 * frontend continues through this service rather than restarting here.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";
	public static final String MDC_KEY = "requestId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String requestId = request.getHeader(HEADER);
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString().substring(0, 8);
		}

		MDC.put(MDC_KEY, requestId);
		// Echoed back so a caller reporting a problem can quote the id.
		response.setHeader(HEADER, requestId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Threads are pooled and reused, so leaving this set would stamp the
			// next unrelated request with this one's id.
			MDC.remove(MDC_KEY);
		}
	}
}
