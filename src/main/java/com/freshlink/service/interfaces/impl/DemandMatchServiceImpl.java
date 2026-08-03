package com.freshlink.service.interfaces.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.DemandStatus;
import com.freshlink.enums.MatchStatus;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.model.DailySupply;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchService;

import lombok.RequiredArgsConstructor;
@Transactional
@Service
@RequiredArgsConstructor
public class DemandMatchServiceImpl implements DemandMatchService{
	
	public final DailySupplyRepository  dailySupplyRepository;
	public final SupplyMatchRepository supplyMatchRepository;
	public final DemandRepository demandRepository;
	
	
	@Override
	public List<SupplyMatch> matchDemand(Long demandId) {
		
		DemandRequest demand = demandRepository.findById(demandId)
	            .orElseThrow(() -> new RuntimeException("Demand not found"));

	    List<DailySupply> supplies =
	        dailySupplyRepository.findAvailableSupplies(
	                demand.getFishType(),
	                demand.getRequiredDate(),
	                SupplyStatus.AVAILABLE
	        );

	    supplies.sort(
	        Comparator
	            .comparing(DailySupply::getFreshnessScore).reversed()
	            .thenComparing(ds -> ds.getSupplier().getAverageRating(), Comparator.reverseOrder())
	            .thenComparing(DailySupply::getCatchDateTime)
	    );

	    List<SupplyMatch> matches = new ArrayList<>();
	    double remaining = demand.getRequestedQuantity();

	    for (DailySupply supply : supplies) {
            if (remaining <= 0) break;

            double allocated = Math.min(remaining, supply.getQuantity());

            SupplyMatch match = new SupplyMatch();
            match.setDemandRequest(demand);
            match.setDailySupply(supply);
            match.setConfirmedQuantity(allocated);
            match.setStatus(MatchStatus.PENDING);  

            matches.add(match);
            remaining -= allocated;
        }
	    
	    if (matches.isEmpty()) {
	        demand.setStatus(DemandStatus.OPEN); 
	        return List.of();
	    }

	    supplyMatchRepository.saveAll(matches);
	    
	    if (remaining == 0) {
            demand.setStatus(DemandStatus.FULLY_MATCHED);
        } else if (!matches.isEmpty()) {
            demand.setStatus(DemandStatus.PARTIALLY_MATCHED);
        } else {
            demand.setStatus(DemandStatus.OPEN);
        }

        demandRepository.save(demand);

        return matches;
	}

}
