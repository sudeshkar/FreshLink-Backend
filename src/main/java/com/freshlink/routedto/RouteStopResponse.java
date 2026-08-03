package com.freshlink.routedto;

import java.time.LocalDateTime;

public record RouteStopResponse(
		Long deliveryId,
		Long orderId,
		String cafeName,
		String cafeAddress,
		String status,
		LocalDateTime expectedAt,
		LocalDateTime deliveredAt
) {
}
