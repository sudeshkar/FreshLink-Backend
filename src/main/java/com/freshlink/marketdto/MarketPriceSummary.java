package com.freshlink.marketdto;

import java.math.BigDecimal;

/** What a fish type is going for across the whole market right now. */
public record MarketPriceSummary(
		Long fishTypeId,
		String fishTypeName,
		BigDecimal lowestPricePerKg,
		BigDecimal highestPricePerKg,
		BigDecimal averagePricePerKg,
		long listingCount,
		double totalAvailableKg,
		boolean inSeason
) {
}
