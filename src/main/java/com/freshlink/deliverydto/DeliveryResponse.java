package com.freshlink.deliverydto;

import java.time.LocalDateTime;

public record DeliveryResponse(
		Long deliveryId,
		Long orderId,
		String status,
		String driverName,
		String driverPhone,
		LocalDateTime expectedAt,
		LocalDateTime deliveredAt,
		String notes
) {
}
