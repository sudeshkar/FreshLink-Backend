package com.freshlink.rating.dto;

import java.time.LocalDateTime;

public record RatingResponse(
		Long ratingId,
		Long orderId,
		String supplierName,
		Integer score,
		String comment,
		LocalDateTime createdAt
) {
}
