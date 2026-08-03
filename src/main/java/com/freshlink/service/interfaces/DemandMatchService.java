package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.model.SupplyMatch;

public interface DemandMatchService {
	
	List<SupplyMatch> matchDemand(Long demandId);

}
