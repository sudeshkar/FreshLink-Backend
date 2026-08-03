package com.freshlink.marketdto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Average price across the market on one day. */
public record PriceTrendPoint(
		LocalDate date,
		BigDecimal averagePricePerKg,
		long priceChanges
) {
}
