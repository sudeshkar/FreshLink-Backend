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
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.model.DailySupply;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchService;

import lombok.RequiredArgsConstructor;

@Transactional
@Service
@RequiredArgsConstructor
public class DemandMatchServiceImpl implements DemandMatchService {

	/** Quantities are doubles; compare against a tolerance rather than zero. */
	private static final double EPSILON = 0.0001;

	private final DailySupplyRepository dailySupplyRepository;
	private final SupplyMatchRepository supplyMatchRepository;
	private final DemandRepository demandRepository;

	@Override
	public List<SupplyMatch> matchDemand(Long demandId) {

		DemandRequest demand = demandRepository.findById(demandId)
				.orElseThrow(() -> new ResourceNotFoundException("Demand request", demandId));

		// Only the shortfall is matched. The scheduler re-runs this every 10 minutes
		// for demand that is still open or partially filled, and starting from the
		// full requested quantity each time would stack a fresh set of matches on
		// top of the existing ones and allocate the demand several times over.
		double remaining = demand.getRequestedQuantity() - matchedQuantity(demand);
		if (remaining <= EPSILON) {
			applyStatus(demand);
			return List.of();
		}

		List<DailySupply> supplies = dailySupplyRepository.findAvailableSupplies(
				demand.getFishType(),
				demand.getRequiredDate(),
				SupplyStatus.AVAILABLE);

		supplies.sort(Comparator
				.comparing(DailySupply::getFreshnessScore).reversed()
				.thenComparing(ds -> ds.getSupplier().getAverageRating(), Comparator.reverseOrder())
				.thenComparing(DailySupply::getCatchDateTime));

		List<SupplyMatch> matches = new ArrayList<>();

		for (DailySupply supply : supplies) {
			if (remaining <= EPSILON) {
				break;
			}

			// A supply's quantity is only decremented when the supplier accepts, so
			// pending matches against it are claims that have not landed yet. Ignoring
			// them lets the same kilos be promised to several cafes at once.
			double available = unclaimedQuantity(supply);
			if (available <= EPSILON) {
				continue;
			}

			double allocated = Math.min(remaining, available);

			SupplyMatch match = new SupplyMatch();
			match.setDemandRequest(demand);
			match.setDailySupply(supply);
			match.setConfirmedQuantity(allocated);
			match.setStatus(MatchStatus.PENDING);

			matches.add(match);
			remaining -= allocated;
		}

		if (!matches.isEmpty()) {
			supplyMatchRepository.saveAll(matches);
		}

		applyStatus(demand);
		return matches;
	}

	@Override
	public void applyStatus(DemandRequest demand) {
		double matched = matchedQuantity(demand);

		DemandStatus status;
		if (matched <= EPSILON) {
			status = DemandStatus.OPEN;
		} else if (matched >= demand.getRequestedQuantity() - EPSILON) {
			status = DemandStatus.FULLY_MATCHED;
		} else {
			status = DemandStatus.PARTIALLY_MATCHED;
		}

		demand.setStatus(status);
		demandRepository.save(demand);
	}

	/** Quantity currently promised to this demand, whether pending or accepted. */
	private double matchedQuantity(DemandRequest demand) {
		return supplyMatchRepository.findByDemandRequest(demand).stream()
				.filter(match -> match.getStatus() == MatchStatus.PENDING
						|| match.getStatus() == MatchStatus.ACCEPTED)
				.mapToDouble(SupplyMatch::getConfirmedQuantity)
				.sum();
	}

	/** Supply quantity not already spoken for by an outstanding pending match. */
	private double unclaimedQuantity(DailySupply supply) {
		double claimed = supplyMatchRepository.findByDailySupply(supply).stream()
				.filter(match -> match.getStatus() == MatchStatus.PENDING)
				.mapToDouble(SupplyMatch::getConfirmedQuantity)
				.sum();

		return supply.getQuantity() - claimed;
	}
}
