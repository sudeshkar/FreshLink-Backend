package com.freshlink.supplydto;

import java.time.LocalDateTime;

public record DailySupplyResponse(
		Long id,
		Long fishTypeId,
		String fishTypeName,
		Double quantity,
		LocalDateTime catchDateTime,
		Double freshnessScore,
		String status
) {
}
