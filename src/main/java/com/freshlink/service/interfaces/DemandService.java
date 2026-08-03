package com.freshlink.service.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.demand.dto.DemandRequestDto;
import com.freshlink.demand.dto.DemandResponse;
import com.freshlink.model.Cafe;

public interface DemandService {
	
	void createDemand(DemandRequestDto dto, Cafe cafe);

	/** Deletes demand owned by {@code cafe}. Other cafes' demand is not visible or deletable. */
	void deleteDemand(Long demandId, Cafe cafe);

	/** Returns only the demand belonging to {@code cafe}. */
	Page<DemandResponse> getDemand(Cafe cafe, Pageable pageable);

}
