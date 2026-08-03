package com.freshlink.demand.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DemandRequestDto(
		@NotNull
        Long fishTypeId,

        @NotNull
        @Min(1)
        Double requestedQuantity,   

        /**
         * Must not be in the past. The matching engine skips past-dated demand and
         * the scheduler closes it, so accepting one only to cancel it minutes later
         * is a confusing way to reject bad input.
         */
        @NotNull
        @FutureOrPresent(message = "Required date cannot be in the past")
        LocalDate requiredDate  
        
		) {

}
