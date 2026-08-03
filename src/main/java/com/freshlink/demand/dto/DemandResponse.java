package com.freshlink.demand.dto;

import java.time.LocalDate;

public record DemandResponse(
		Long id,
		Long fishId,
		String fishName,
		Double requestedQuantity,
		LocalDate requiredDate,
		String demandStatus
		
		) {

}
