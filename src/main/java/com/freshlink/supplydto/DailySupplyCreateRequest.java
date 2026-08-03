package com.freshlink.supplydto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A supplier recording the day's catch, which the matching engine allocates
 * against outstanding cafe demand.
 */
public record DailySupplyCreateRequest(

		@NotBlank(message = "Fish type is required")
		String fishTypeName,

		@NotNull(message = "Quantity is required")
		@Positive(message = "Quantity must be greater than zero")
		Double quantity,

		@NotNull(message = "Catch date and time is required")
		LocalDateTime catchDateTime,

		/** Drives match ranking - freshest supply is allocated first. */
		@NotNull(message = "Freshness score is required")
		@DecimalMin(value = "0.0", message = "Freshness score must be between 0 and 1")
		@DecimalMax(value = "1.0", message = "Freshness score must be between 0 and 1")
		Double freshnessScore
) {
}
