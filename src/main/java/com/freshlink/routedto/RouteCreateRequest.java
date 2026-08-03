package com.freshlink.routedto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A van-load: one driver, one day, several drop-offs. */
public record RouteCreateRequest(

		@NotNull(message = "Route date is required")
		LocalDate routeDate,

		@Size(max = 255) String driverName,
		@Size(max = 255) String driverPhone,
		@Size(max = 500) String notes,

		/** Orders to drop off. Each must already be out for delivery. */
		@NotEmpty(message = "A route needs at least one order")
		List<Long> orderIds
) {
}
