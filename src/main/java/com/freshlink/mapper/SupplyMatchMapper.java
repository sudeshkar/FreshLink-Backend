package com.freshlink.mapper;

import com.freshlink.model.SupplyMatch;
import com.freshlink.supplymatch.dto.SupplyMatchResponse;

public class SupplyMatchMapper {
	
	public static SupplyMatchResponse toResponse(SupplyMatch match) {

	    return new SupplyMatchResponse(
	        match.getId(),
	        match.getStatus(),
	        match.getConfirmedQuantity(),

	        match.getDemandRequest().getId(),
	        match.getDemandRequest().getCafe().getName(),
	        match.getDemandRequest().getFishType().getName(),
	        match.getDemandRequest().getRequestedQuantity(),
	        match.getDemandRequest().getRequiredDate(),

	        match.getDailySupply().getId(),
	        match.getDailySupply().getSupplier().getName(),
	        match.getDailySupply().getQuantity(),
	        match.getDailySupply().getCatchDateTime(),
	        match.getDailySupply().getFreshnessScore()
	    );
	}


}
