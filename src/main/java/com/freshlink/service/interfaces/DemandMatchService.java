package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.model.DemandRequest;
import com.freshlink.model.SupplyMatch;

public interface DemandMatchService {

	/**
	 * Allocates available supply against the demand's outstanding shortfall.
	 * Safe to call repeatedly: quantity already matched is not re-allocated.
	 */
	List<SupplyMatch> matchDemand(Long demandId);

	/**
	 * Recomputes the demand's status from the quantity its live matches cover.
	 * Call after anything that changes those matches.
	 */
	void applyStatus(DemandRequest demand);
}
