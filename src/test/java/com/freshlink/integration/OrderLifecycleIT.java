package com.freshlink.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
// Spring Boot 4 moved this out of org.springframework.boot.test.autoconfigure
// into the per-technology webmvc-test module.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

// JsonPath rather than an injected ObjectMapper: Spring Boot 4 defaults to
// Jackson 3, so there is no com.fasterxml.jackson ObjectMapper bean to autowire.
import com.jayway.jsonpath.JsonPath;

/**
 * Drives the spot-market order lifecycle over real HTTP against the migrated
 * schema and the demo data AdminBootstrap seeds.
 *
 * The service-level unit tests mock their repositories, so they cannot catch a
 * broken mapping, a filter ordering problem, or a rule that only fails once
 * Hibernate flushes. This covers the path a client actually takes.
 */
// Rate limiting is switched off here: every request comes from the same
// loopback address, so the 5-per-15-minutes login cap would throttle the suite
// itself rather than testing anything. RateLimitServiceTest covers the limiter.
@SpringBootTest(properties = "app.ratelimit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OrderLifecycleIT {

	@Autowired private MockMvc mockMvc;

	private static final String CAFE = "cafe1@freshlink.com";
	private static final String SUPPLIER = "supplier1@freshlink.com";
	private static final String PASSWORD = "password";

	private String tokenFor(String email) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("""
								{"email":"%s","password":"%s"}""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return JsonPath.read(body, "$.accessToken");
	}

	private long firstMarketFishId(String cafeToken) throws Exception {
		String body = mockMvc.perform(get("/api/v1/cafes/market/fish")
						.header("Authorization", "Bearer " + cafeToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return ((Number) JsonPath.read(body, "$.content[0].id")).longValue();
	}

	@Test
	@DisplayName("cafe orders, supplier accepts, delivers and completes, cafe rates")
	void fullOrderLifecycle() throws Exception {
		String cafeToken = tokenFor(CAFE);
		String supplierToken = tokenFor(SUPPLIER);

		long fishId = firstMarketFishId(cafeToken);

		String orderBody = mockMvc.perform(post("/api/v1/cafes/orders")
						.header("Authorization", "Bearer " + cafeToken)
						.contentType("application/json")
						.content("""
								{"items":[{"fishId":%d,"quantityKg":5.0}]}""".formatted(fishId)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		long orderId = ((Number) JsonPath.read(orderBody, "$.orderId")).longValue();

		mockMvc.perform(put("/api/v1/suppliers/orders/" + orderId + "/accept")
						.header("Authorization", "Bearer " + supplierToken))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/suppliers/orders/" + orderId + "/markdelivering")
						.header("Authorization", "Bearer " + supplierToken))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/suppliers/orders/" + orderId + "/complete")
						.header("Authorization", "Bearer " + supplierToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/cafes/orders/" + orderId + "/rate")
						.header("Authorization", "Bearer " + cafeToken)
						.contentType("application/json")
						.content("""
								{"score":5,"comment":"Fresh and on time"}"""))
				.andExpect(status().isOk());

		// A second rating on the same order must be refused. 409, not 400: the
		// request is perfectly well formed, it just conflicts with existing state.
		mockMvc.perform(post("/api/v1/cafes/orders/" + orderId + "/rate")
						.header("Authorization", "Bearer " + cafeToken)
						.contentType("application/json")
						.content("""
								{"score":1,"comment":"Changed my mind"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("a completed order can no longer be cancelled")
	void cannotCancelACompletedOrder() throws Exception {
		String cafeToken = tokenFor(CAFE);
		String supplierToken = tokenFor(SUPPLIER);
		long fishId = firstMarketFishId(cafeToken);

		String orderBody = mockMvc.perform(post("/api/v1/cafes/orders")
						.header("Authorization", "Bearer " + cafeToken)
						.contentType("application/json")
						.content("""
								{"items":[{"fishId":%d,"quantityKg":2.0}]}""".formatted(fishId)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		long orderId = ((Number) JsonPath.read(orderBody, "$.orderId")).longValue();

		mockMvc.perform(put("/api/v1/suppliers/orders/" + orderId + "/accept")
				.header("Authorization", "Bearer " + supplierToken)).andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/cafes/orders/" + orderId + "/cancel")
						.header("Authorization", "Bearer " + cafeToken))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("ordering more than is in stock is refused")
	void cannotOrderMoreThanAvailable() throws Exception {
		String cafeToken = tokenFor(CAFE);
		long fishId = firstMarketFishId(cafeToken);

		mockMvc.perform(post("/api/v1/cafes/orders")
						.header("Authorization", "Bearer " + cafeToken)
						.contentType("application/json")
						.content("""
								{"items":[{"fishId":%d,"quantityKg":999999.0}]}""".formatted(fishId)))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("list endpoints are paged")
	void listEndpointsArePaged() throws Exception {
		String cafeToken = tokenFor(CAFE);

		mockMvc.perform(get("/api/v1/cafes/market/fish?page=0&size=1")
						.header("Authorization", "Bearer " + cafeToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").exists())
				.andExpect(jsonPath("$.totalElements").exists())
				.andExpect(jsonPath("$.content.length()").value(is(1)))
				.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
	}
}
