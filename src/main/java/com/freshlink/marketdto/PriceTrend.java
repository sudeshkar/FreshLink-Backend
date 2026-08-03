package com.freshlink.marketdto;

import java.math.BigDecimal;
import java.util.List;

public record PriceTrend(
		Long fishTypeId,
		String fishTypeName,
		int windowDays,
		BigDecimal periodLow,
		BigDecimal periodHigh,
		List<PriceTrendPoint> points
) {
}
