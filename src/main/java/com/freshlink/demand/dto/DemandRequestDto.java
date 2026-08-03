package com.freshlink.demand.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DemandRequestDto(
		@NotNull
        Long fishTypeId,

        @NotNull
        @Min(1)
        Double requestedQuantity,   

        @NotNull
        LocalDate requiredDate  
        
		) {

}
