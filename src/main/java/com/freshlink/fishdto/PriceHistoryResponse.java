package com.freshlink.fishdto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryResponse(
		BigDecimal pricePerKg,
		LocalDateTime recordedAt
) {
}
