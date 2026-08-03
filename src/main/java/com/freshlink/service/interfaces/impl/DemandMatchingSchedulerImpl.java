package com.freshlink.service.interfaces.impl;

import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.freshlink.Repository.DemandRepository;
import com.freshlink.enums.DemandStatus;
import com.freshlink.model.DemandRequest;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;

import lombok.RequiredArgsConstructor;


@EnableScheduling
@Service
@RequiredArgsConstructor
public class DemandMatchingSchedulerImpl implements DemandMatchingScheduler{
	
	private final DemandRepository demandRepository;
	private final DemandMatchService demandMatchService;
	
	@Scheduled(fixedRate = 600000)
	@Override
	public void autoMatchDemands() {
			List<DemandRequest> openDemands =
		        demandRepository.findByStatusIn(List.of(DemandStatus.OPEN, DemandStatus.PARTIALLY_MATCHED));

		    for (DemandRequest demand : openDemands) {
		        demandMatchService.matchDemand(demand.getId());
		    }
		
	}
	 

}
