package com.freshlink.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.MatchStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.model.Cafe;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.impl.DemandServiceImpl;

/**
 * Demand used to be readable and deletable across cafes: the listing called
 * findAll() and the delete had no ownership check at all.
 */
@ExtendWith(MockitoExtension.class)
class DemandServiceOwnershipTest {

	@Mock private FishTypeRepository fishTypeRepository;
	@Mock private DemandRepository demandRepository;
	@Mock private SupplyMatchRepository supplyMatchRepository;
	@Mock private DemandMatchService demandMatchService;

	@InjectMocks private DemandServiceImpl demandService;

	private Cafe cafe(Long id, String email) {
		Cafe cafe = new Cafe();
		cafe.setId(id);
		cafe.setEmail(email);
		return cafe;
	}

	private DemandRequest demandOwnedBy(Cafe owner) {
		DemandRequest demand = new DemandRequest();
		demand.setId(50L);
		demand.setCafe(owner);
		return demand;
	}

	@Test
	@DisplayName("a cafe cannot delete another cafe's demand")
	void deleteRejectsForeignDemand() {
		Cafe owner = cafe(1L, "owner@cafe.test");
		Cafe attacker = cafe(2L, "attacker@cafe.test");
		when(demandRepository.findById(50L)).thenReturn(Optional.of(demandOwnedBy(owner)));

		// 404, not 403 - a 403 would confirm the id exists.
		assertThatThrownBy(() -> demandService.deleteDemand(50L, attacker))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(demandRepository, never()).delete(any());
		verify(supplyMatchRepository, never()).deleteAll(any());
	}

	@Test
	@DisplayName("demand with an accepted match cannot be deleted")
	void deleteRejectsAcceptedMatch() {
		Cafe owner = cafe(1L, "owner@cafe.test");
		DemandRequest demand = demandOwnedBy(owner);

		SupplyMatch accepted = new SupplyMatch();
		accepted.setStatus(MatchStatus.ACCEPTED);

		when(demandRepository.findById(50L)).thenReturn(Optional.of(demand));
		when(supplyMatchRepository.findByDemandRequest(demand)).thenReturn(List.of(accepted));

		assertThatThrownBy(() -> demandService.deleteDemand(50L, owner))
				.isInstanceOf(BusinessRuleException.class);

		verify(demandRepository, never()).delete(any());
	}

	@Test
	@DisplayName("owner deletes own demand, clearing its pending matches first")
	void deleteClearsPendingMatches() {
		Cafe owner = cafe(1L, "owner@cafe.test");
		DemandRequest demand = demandOwnedBy(owner);

		SupplyMatch pending = new SupplyMatch();
		pending.setStatus(MatchStatus.PENDING);
		List<SupplyMatch> matches = List.of(pending);

		when(demandRepository.findById(50L)).thenReturn(Optional.of(demand));
		when(supplyMatchRepository.findByDemandRequest(demand)).thenReturn(matches);

		demandService.deleteDemand(50L, owner);

		// Matches must go first - SupplyMatch has an unCascaded FK to DemandRequest.
		verify(supplyMatchRepository).deleteAll(matches);
		verify(demandRepository).delete(demand);
	}

	@Test
	@DisplayName("listing demand is scoped to the calling cafe, never findAll")
	void listingIsScopedToCafe() {
		Cafe cafe = cafe(1L, "owner@cafe.test");
		Pageable pageable = PageRequest.of(0, 20);
		when(demandRepository.findByCafe(cafe, pageable)).thenReturn(Page.empty());

		demandService.getDemand(cafe, pageable);

		verify(demandRepository).findByCafe(cafe, pageable);
		verify(demandRepository, never()).findAll();
	}
}
