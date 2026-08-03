package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.DemandStatus;
import com.freshlink.enums.MatchStatus;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.model.Cafe;
import com.freshlink.model.DailySupply;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.impl.DemandMatchServiceImpl;

@ExtendWith(MockitoExtension.class)
class DemandMatchServiceTest {

	@Mock private DailySupplyRepository dailySupplyRepository;
	@Mock private SupplyMatchRepository supplyMatchRepository;
	@Mock private DemandRepository demandRepository;

	@InjectMocks private DemandMatchServiceImpl service;

	private final FishType tuna = fishType();

	private FishType fishType() {
		FishType type = new FishType();
		type.setId(1L);
		type.setName("Tuna");
		return type;
	}

	private DemandRequest demand(double requested) {
		Cafe cafe = new Cafe();
		cafe.setId(1L);

		DemandRequest demand = new DemandRequest();
		demand.setId(10L);
		demand.setCafe(cafe);
		demand.setFishType(tuna);
		demand.setRequestedQuantity(requested);
		demand.setRequiredDate(LocalDate.now().plusDays(3));
		demand.setStatus(DemandStatus.OPEN);
		return demand;
	}

	private DailySupply supply(Long id, double quantity) {
		Supplier supplier = new Supplier();
		supplier.setId(id);
		supplier.setAverageRating(4.0);

		DailySupply supply = new DailySupply();
		supply.setId(id);
		supply.setSupplier(supplier);
		supply.setFishType(tuna);
		supply.setQuantity(quantity);
		supply.setFreshnessScore(0.9);
		supply.setCatchDateTime(LocalDateTime.now().minusHours(2));
		supply.setStatus(SupplyStatus.AVAILABLE);
		return supply;
	}

	private SupplyMatch match(DemandRequest demand, DailySupply supply, double qty, MatchStatus status) {
		SupplyMatch match = new SupplyMatch();
		match.setDemandRequest(demand);
		match.setDailySupply(supply);
		match.setConfirmedQuantity(qty);
		match.setStatus(status);
		return match;
	}

	@Test
	@DisplayName("re-matching allocates only the shortfall, not the whole request again")
	void reMatchingOnlyCoversTheShortfall() {
		DemandRequest demand = demand(100.0);
		DailySupply existingSource = supply(1L, 40.0);
		DailySupply fresh = supply(2L, 500.0);

		// 40 of the 100 is already covered by a pending match.
		when(demandRepository.findById(10L)).thenReturn(Optional.of(demand));
		when(supplyMatchRepository.findByDemandRequest(demand))
				.thenReturn(List.of(match(demand, existingSource, 40.0, MatchStatus.PENDING)));
		when(dailySupplyRepository.findAvailableSupplies(any(), any(), eq(SupplyStatus.AVAILABLE)))
				.thenReturn(new java.util.ArrayList<>(List.of(fresh)));
		when(supplyMatchRepository.findByDailySupply(fresh)).thenReturn(List.of());

		List<SupplyMatch> created = service.matchDemand(10L);

		assertThat(created).hasSize(1);
		assertThat(created.get(0).getConfirmedQuantity())
				.as("only the outstanding 60 should be allocated, not another 100")
				.isEqualTo(60.0);
	}

	@Test
	@DisplayName("a fully matched demand creates nothing on a repeat pass")
	void fullyMatchedDemandIsLeftAlone() {
		DemandRequest demand = demand(100.0);
		DailySupply source = supply(1L, 100.0);

		when(demandRepository.findById(10L)).thenReturn(Optional.of(demand));
		when(supplyMatchRepository.findByDemandRequest(demand))
				.thenReturn(List.of(match(demand, source, 100.0, MatchStatus.PENDING)));

		List<SupplyMatch> created = service.matchDemand(10L);

		assertThat(created).isEmpty();
		verify(supplyMatchRepository, never()).saveAll(any());
		assertThat(demand.getStatus()).isEqualTo(DemandStatus.FULLY_MATCHED);
	}

	@Test
	@DisplayName("supply already claimed by a pending match is not promised twice")
	void supplyIsNotOverAllocated() {
		DemandRequest demand = demand(100.0);
		DailySupply shared = supply(1L, 50.0);

		// Mirrors the repository: matches saved during the run are visible to the
		// status recomputation that follows, exactly as an auto-flush would make them.
		List<SupplyMatch> persisted = new java.util.ArrayList<>();

		when(demandRepository.findById(10L)).thenReturn(Optional.of(demand));
		when(supplyMatchRepository.findByDemandRequest(demand)).thenReturn(persisted);
		when(supplyMatchRepository.saveAll(any())).thenAnswer(inv -> {
			persisted.addAll(inv.getArgument(0));
			return inv.getArgument(0);
		});
		when(dailySupplyRepository.findAvailableSupplies(any(), any(), eq(SupplyStatus.AVAILABLE)))
				.thenReturn(new java.util.ArrayList<>(List.of(shared)));
		// 30 of this supply's 50 kg is already promised to a different cafe.
		when(supplyMatchRepository.findByDailySupply(shared))
				.thenReturn(List.of(match(demand(30.0), shared, 30.0, MatchStatus.PENDING)));

		List<SupplyMatch> created = service.matchDemand(10L);

		assertThat(created).hasSize(1);
		assertThat(created.get(0).getConfirmedQuantity())
				.as("only the unclaimed 20 kg is available")
				.isEqualTo(20.0);
		assertThat(demand.getStatus())
				.as("20 of 100 covered")
				.isEqualTo(DemandStatus.PARTIALLY_MATCHED);
	}

	@Test
	@DisplayName("fully claimed supply is skipped entirely")
	void fullyClaimedSupplyIsSkipped() {
		DemandRequest demand = demand(100.0);
		DailySupply exhausted = supply(1L, 50.0);

		when(demandRepository.findById(10L)).thenReturn(Optional.of(demand));
		when(supplyMatchRepository.findByDemandRequest(demand)).thenReturn(List.of());
		when(dailySupplyRepository.findAvailableSupplies(any(), any(), eq(SupplyStatus.AVAILABLE)))
				.thenReturn(new java.util.ArrayList<>(List.of(exhausted)));
		when(supplyMatchRepository.findByDailySupply(exhausted))
				.thenReturn(List.of(match(demand(50.0), exhausted, 50.0, MatchStatus.PENDING)));

		assertThat(service.matchDemand(10L)).isEmpty();
		assertThat(demand.getStatus()).isEqualTo(DemandStatus.OPEN);
	}

	@Test
	@DisplayName("status is recomputed from live matches, ignoring rejected ones")
	void statusIgnoresRejectedMatches() {
		DemandRequest demand = demand(100.0);
		DailySupply source = supply(1L, 100.0);

		when(supplyMatchRepository.findByDemandRequest(demand)).thenReturn(List.of(
				match(demand, source, 60.0, MatchStatus.PENDING),
				match(demand, source, 40.0, MatchStatus.REJECTED)));

		service.applyStatus(demand);

		assertThat(demand.getStatus())
				.as("only the 60 kg still live counts toward coverage")
				.isEqualTo(DemandStatus.PARTIALLY_MATCHED);
	}

	@Test
	@DisplayName("a demand whose only match was rejected returns to OPEN")
	void allRejectedReturnsToOpen() {
		DemandRequest demand = demand(100.0);
		DailySupply source = supply(1L, 100.0);
		demand.setStatus(DemandStatus.FULLY_MATCHED);

		when(supplyMatchRepository.findByDemandRequest(demand))
				.thenReturn(List.of(match(demand, source, 100.0, MatchStatus.REJECTED)));

		service.applyStatus(demand);

		assertThat(demand.getStatus()).isEqualTo(DemandStatus.OPEN);
	}
}
