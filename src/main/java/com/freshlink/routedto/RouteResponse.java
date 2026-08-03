package com.freshlink.routedto;

import java.time.LocalDate;
import java.util.List;

public record RouteResponse(
		Long routeId,
		LocalDate routeDate,
		String driverName,
		String driverPhone,
		String status,
		String notes,
		int stopCount,
		List<RouteStopResponse> stops
) {
}
