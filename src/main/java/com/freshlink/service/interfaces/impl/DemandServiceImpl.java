package com.freshlink.service.interfaces.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.demand.dto.DemandRequestDto;
import com.freshlink.demand.dto.DemandResponse;
import com.freshlink.enums.DemandStatus;
import com.freshlink.enums.MatchStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.model.Cafe;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class DemandServiceImpl implements DemandService{

	private final FishTypeRepository fishTypeRepository;
	private final DemandRepository	demandRepository;
	private final SupplyMatchRepository supplyMatchRepository;
	private final DemandMatchService demandMatchService;

	@Override
	public void createDemand(DemandRequestDto dto, Cafe cafe) {
		DemandRequest demand = new DemandRequest();
		demand.setCafe(cafe);
		demand.setFishType(
				fishTypeRepository.findById(dto.fishTypeId())
		        .orElseThrow(() -> new ResourceNotFoundException("Fish type", dto.fishTypeId()))
		);
		demand.setRequestedQuantity(dto.requestedQuantity());
		demand.setRequiredDate(dto.requiredDate());
		demand.setStatus(DemandStatus.OPEN);
		
		DemandRequest saved = demandRepository.save(demand);
		demandMatchService.matchDemand(saved.getId());

	}

	@Override
	@Transactional
	public void deleteDemand(Long demandId, Cafe cafe) {
		DemandRequest demand = demandRepository.findById(demandId)
				.filter(d -> d.getCafe().getId().equals(cafe.getId()))
				// 404 rather than 403 on an ownership mismatch: a 403 would confirm that
				// the id exists, letting one cafe enumerate another's demand.
				.orElseThrow(() -> new ResourceNotFoundException("Demand request", demandId));

		List<SupplyMatch> matches = supplyMatchRepository.findByDemandRequest(demand);

		boolean hasAcceptedMatch = matches.stream()
				.anyMatch(match -> match.getStatus() == MatchStatus.ACCEPTED);
		if (hasAcceptedMatch) {
			throw new BusinessRuleException(
					"This demand has already been accepted by a supplier and can no longer be deleted.");
		}

		// Supply is only deducted when a supplier accepts, so pending/rejected matches
		// hold no stock and can simply go. They must be removed first regardless -
		// SupplyMatch has a FK to DemandRequest with no cascade.
		supplyMatchRepository.deleteAll(matches);
		demandRepository.delete(demand);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DemandResponse> getDemand(Cafe cafe, Pageable pageable) {
		return demandRepository.findByCafe(cafe, pageable).map(this::toResponse);
	}

	private DemandResponse toResponse(DemandRequest demandRequest) {
		return new DemandResponse(
				demandRequest.getId(),
				demandRequest.getFishType().getId(),
				demandRequest.getFishType().getName(),
				demandRequest.getRequestedQuantity(),
				demandRequest.getRequiredDate(),
				demandRequest.getStatus().toString()
		);
	}
}
