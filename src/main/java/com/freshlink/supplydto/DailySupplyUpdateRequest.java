package com.freshlink.supplydto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/** Partial update - null fields are left unchanged. */
public record DailySupplyUpdateRequest(

		@Positive(message = "Quantity must be greater than zero")
		Double quantity,

		@DecimalMin(value = "0.0", message = "Freshness score must be between 0 and 1")
		@DecimalMax(value = "1.0", message = "Freshness score must be between 0 and 1")
		Double freshnessScore
) {
}
