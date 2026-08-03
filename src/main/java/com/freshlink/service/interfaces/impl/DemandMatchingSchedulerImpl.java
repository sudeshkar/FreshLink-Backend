package com.freshlink.service.interfaces.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.DemandStatus;
import com.freshlink.enums.MatchStatus;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;

import lombok.RequiredArgsConstructor;

@EnableScheduling
@Service
@RequiredArgsConstructor
public class DemandMatchingSchedulerImpl implements DemandMatchingScheduler {

	private static final Logger log = LoggerFactory.getLogger(DemandMatchingSchedulerImpl.class);

	private static final List<DemandStatus> UNFILLED =
			List.of(DemandStatus.OPEN, DemandStatus.PARTIALLY_MATCHED);

	private final DemandRepository demandRepository;
	private final SupplyMatchRepository supplyMatchRepository;
	private final DemandMatchService demandMatchService;

	/** How long a supplier has to answer before the claim is released. */
	@Value("${app.matching.pending-timeout-hours:24}")
	private long pendingTimeoutHours;

	@Scheduled(fixedRate = 600_000)
	@Override
	@Transactional
	public void autoMatchDemands() {
		// Demand whose delivery date has passed can never be met, so retrying it
		// every ten minutes forever is pure waste. expireStaleMatches closes it.
		List<DemandRequest> matchable = demandRepository
				.findByStatusInAndRequiredDateGreaterThanEqual(UNFILLED, LocalDate.now());

		for (DemandRequest demand : matchable) {
			demandMatchService.matchDemand(demand.getId());
		}
	}

	@Scheduled(fixedRate = 600_000)
	@Override
	@Transactional
	public void expireStaleMatches() {
		releaseAbandonedMatches();
		closePastDatedDemand();
	}

	/**
	 * A pending match reserves part of a supply, so one the supplier never acts on
	 * keeps that quantity out of circulation indefinitely - the supply cannot be
	 * offered elsewhere and the demand behind it never completes. Expiring the
	 * match returns the quantity to the pool, and the next matching pass can
	 * reallocate it to someone who will actually respond.
	 */
	private void releaseAbandonedMatches() {
		LocalDateTime cutoff = LocalDateTime.now().minusHours(pendingTimeoutHours);

		List<SupplyMatch> abandoned =
				supplyMatchRepository.findByStatusAndCreatedAtBefore(MatchStatus.PENDING, cutoff);

		if (abandoned.isEmpty()) {
			return;
		}

		Set<DemandRequest> affected = new HashSet<>();
		for (SupplyMatch match : abandoned) {
			match.setStatus(MatchStatus.EXPIRED);
			affected.add(match.getDemandRequest());
		}
		supplyMatchRepository.saveAll(abandoned);

		// Coverage dropped, so each affected demand needs its status recomputed -
		// otherwise a demand stays FULLY_MATCHED against matches that no longer count.
		affected.forEach(demandMatchService::applyStatus);

		log.info("Expired {} unanswered match(es) across {} demand request(s)",
				abandoned.size(), affected.size());
	}

	private void closePastDatedDemand() {
		List<DemandRequest> stale =
				demandRepository.findByStatusInAndRequiredDateBefore(UNFILLED, LocalDate.now());

		if (stale.isEmpty()) {
			return;
		}

		stale.forEach(demand -> demand.setStatus(DemandStatus.CANCELLED));
		demandRepository.saveAll(stale);

		log.info("Closed {} demand request(s) past their delivery date", stale.size());
	}
}
