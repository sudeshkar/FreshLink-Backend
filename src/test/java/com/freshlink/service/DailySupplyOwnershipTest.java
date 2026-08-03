package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.MatchStatus;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.DailySupply;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;
import com.freshlink.supplydto.DailySupplyCreateRequest;
import com.freshlink.supplydto.DailySupplyUpdateRequest;

/**
 * Daily supply is the matching engine's input, so a supplier editing another
 * supplier's catch would redirect trade that is not theirs.
 */
@ExtendWith(MockitoExtension.class)
class DailySupplyOwnershipTest {

	@Mock private FishRepository fishRepository;
	@Mock private SupplierRepository supplierRepository;
	@Mock private FishMapper fishMapper;
	@Mock private FishTypeRepository fishTypeRepository;
	@Mock private SupplierMapper supplierMapper;
	@Mock private OrderRepository orderRepository;
	@Mock private OrderMapper orderMapper;
	@Mock private SupplyMatchRepository supplyMatchRepository;
	@Mock private DailySupplyRepository dailySupplyRepository;
	@Mock private DemandRepository demandRepository;
	@Mock private DeliveryRepository deliveryRepository;
	@Mock private DemandMatchingScheduler demandMatchingScheduler;

	@Mock private ApplicationEventPublisher events;

	@InjectMocks private SupplierServiceImpl supplierService;

	private Supplier supplier(Long id, String email) {
		Supplier supplier = new Supplier();
		supplier.setId(id);
		supplier.setEmail(email);
		return supplier;
	}

	private DailySupply supplyOwnedBy(Supplier owner, double quantity) {
		FishType fishType = new FishType();
		fishType.setId(1L);
		fishType.setName("Tuna");

		DailySupply supply = new DailySupply();
		supply.setId(7L);
		supply.setSupplier(owner);
		supply.setFishType(fishType);
		supply.setQuantity(quantity);
		supply.setStatus(SupplyStatus.AVAILABLE);
		supply.setFreshnessScore(0.9);
		supply.setCatchDateTime(LocalDateTime.now().minusHours(2));
		return supply;
	}

	@Test
	@DisplayName("a supplier cannot update another supplier's daily catch")
	void updateRejectsForeignSupply() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		Supplier attacker = supplier(2L, "attacker@supplier.test");
		DailySupply supply = supplyOwnedBy(owner, 100.0);

		when(supplierRepository.findByEmail("attacker@supplier.test")).thenReturn(Optional.of(attacker));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));

		assertThatThrownBy(() -> supplierService.updateDailySupply(
				7L, new DailySupplyUpdateRequest(5.0, null), "attacker@supplier.test"))
				.isInstanceOf(ResourceNotFoundException.class);

		assertThat(supply.getQuantity()).isEqualTo(100.0);
		verify(dailySupplyRepository, never()).save(any());
	}

	@Test
	@DisplayName("a supplier cannot delete another supplier's daily catch")
	void deleteRejectsForeignSupply() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		Supplier attacker = supplier(2L, "attacker@supplier.test");

		when(supplierRepository.findByEmail("attacker@supplier.test")).thenReturn(Optional.of(attacker));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supplyOwnedBy(owner, 100.0)));

		assertThatThrownBy(() -> supplierService.deleteDailySupply(7L, "attacker@supplier.test"))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(dailySupplyRepository, never()).delete(any());
	}

	@Test
	@DisplayName("quantity cannot be cut below what a supplier already accepted")
	void updateRefusesToUndercutAcceptedMatches() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		DailySupply supply = supplyOwnedBy(owner, 100.0);

		SupplyMatch accepted = new SupplyMatch();
		accepted.setStatus(MatchStatus.ACCEPTED);
		accepted.setConfirmedQuantity(60.0);

		when(supplierRepository.findByEmail("owner@supplier.test")).thenReturn(Optional.of(owner));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));
		when(supplyMatchRepository.findByDailySupply(supply)).thenReturn(List.of(accepted));

		assertThatThrownBy(() -> supplierService.updateDailySupply(
				7L, new DailySupplyUpdateRequest(20.0, null), "owner@supplier.test"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already accepted");

		assertThat(supply.getQuantity()).isEqualTo(100.0);
	}

	@Test
	@DisplayName("supply with an accepted match cannot be deleted")
	void deleteRefusesWhenMatchAccepted() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		DailySupply supply = supplyOwnedBy(owner, 100.0);

		SupplyMatch accepted = new SupplyMatch();
		accepted.setStatus(MatchStatus.ACCEPTED);

		when(supplierRepository.findByEmail("owner@supplier.test")).thenReturn(Optional.of(owner));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));
		when(supplyMatchRepository.findByDailySupply(supply)).thenReturn(List.of(accepted));

		assertThatThrownBy(() -> supplierService.deleteDailySupply(7L, "owner@supplier.test"))
				.isInstanceOf(BusinessRuleException.class);

		verify(dailySupplyRepository, never()).delete(any());
	}

	@Test
	@DisplayName("catch time in the future is rejected")
	void createRejectsFutureCatchTime() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		FishType tuna = new FishType();
		tuna.setName("Tuna");

		when(supplierRepository.findByEmail("owner@supplier.test")).thenReturn(Optional.of(owner));
		when(fishTypeRepository.findByNameIgnoreCase("Tuna")).thenReturn(Optional.of(tuna));

		assertThatThrownBy(() -> supplierService.addDailySupply(
				new DailySupplyCreateRequest("Tuna", 10.0, LocalDateTime.now().plusDays(1), 0.9),
				"owner@supplier.test"))
				.isInstanceOf(BusinessRuleException.class);

		verify(dailySupplyRepository, never()).save(any());
	}

	@Test
	@DisplayName("recording a catch triggers matching immediately, not on the next scheduled pass")
	void createTriggersMatching() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		FishType tuna = new FishType();
		tuna.setId(1L);
		tuna.setName("Tuna");

		when(supplierRepository.findByEmail("owner@supplier.test")).thenReturn(Optional.of(owner));
		when(fishTypeRepository.findByNameIgnoreCase("Tuna")).thenReturn(Optional.of(tuna));
		// A catch credits the supplier's listing, so one has to exist.
		com.freshlink.model.Fish listing = new com.freshlink.model.Fish();
		listing.setSupplier(owner);
		listing.setFishType(tuna);
		listing.setAvailableKg(0);
		when(fishRepository.findBySupplierAndFishType(owner, tuna)).thenReturn(Optional.of(listing));
		when(dailySupplyRepository.save(any())).thenAnswer(inv -> {
			DailySupply saved = inv.getArgument(0);
			saved.setId(7L);
			return saved;
		});

		supplierService.addDailySupply(
				new DailySupplyCreateRequest("Tuna", 10.0, LocalDateTime.now().minusHours(1), 0.9),
				"owner@supplier.test");

		verify(demandMatchingScheduler).autoMatchDemands();
	}
}
