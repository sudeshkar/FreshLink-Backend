package com.freshlink.analytics.dto;

import java.time.LocalDateTime;

public record CafeAnalyticsDTO(
		Long cafeId,
        String cafeName,
        long totalOrders,
        LocalDateTime lastOrderDate
		) {

}
