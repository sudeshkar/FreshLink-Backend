package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.MatchStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.DailySupply;
import com.freshlink.model.Supplier;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;

/**
 * acceptMatch accepted a supplierEmail argument and never used it, so any
 * supplier could accept another supplier's match - draining their stock and
 * creating an order in their name.
 */
@ExtendWith(MockitoExtension.class)
class SupplierServiceOwnershipTest {

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

	@InjectMocks private SupplierServiceImpl supplierService;

	private Supplier supplier(Long id, String email) {
		Supplier supplier = new Supplier();
		supplier.setId(id);
		supplier.setEmail(email);
		return supplier;
	}

	private SupplyMatch pendingMatchOwnedBy(Supplier owner, double supplyQuantity) {
		DailySupply supply = new DailySupply();
		supply.setSupplier(owner);
		supply.setQuantity(supplyQuantity);

		SupplyMatch match = new SupplyMatch();
		match.setId(9L);
		match.setDailySupply(supply);
		match.setStatus(MatchStatus.PENDING);
		match.setConfirmedQuantity(30.0);
		return match;
	}

	@Test
	@DisplayName("a supplier cannot accept another supplier's match")
	void acceptRejectsForeignMatch() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		Supplier attacker = supplier(2L, "attacker@supplier.test");
		SupplyMatch match = pendingMatchOwnedBy(owner, 100.0);

		when(supplierRepository.findByEmail("attacker@supplier.test")).thenReturn(Optional.of(attacker));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));

		assertThatThrownBy(() -> supplierService.acceptMatch(9L, "attacker@supplier.test"))
				.isInstanceOf(ResourceNotFoundException.class);

		// No stock drained from the real owner, no order forged in their name.
		assertThat(match.getDailySupply().getQuantity()).isEqualTo(100.0);
		assertThat(match.getStatus()).isEqualTo(MatchStatus.PENDING);
		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("a supplier cannot reject another supplier's match")
	void rejectRejectsForeignMatch() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		Supplier attacker = supplier(2L, "attacker@supplier.test");
		SupplyMatch match = pendingMatchOwnedBy(owner, 100.0);

		when(supplierRepository.findByEmail("attacker@supplier.test")).thenReturn(Optional.of(attacker));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));

		assertThatThrownBy(() -> supplierService.rejectMatch(9L, "attacker@supplier.test"))
				.isInstanceOf(ResourceNotFoundException.class);

		assertThat(match.getStatus()).isEqualTo(MatchStatus.PENDING);
	}

	@Test
	@DisplayName("an already-processed match cannot be rejected, which would reopen a filled demand")
	void rejectRefusesNonPendingMatch() {
		Supplier owner = supplier(1L, "owner@supplier.test");
		SupplyMatch match = pendingMatchOwnedBy(owner, 100.0);
		match.setStatus(MatchStatus.ACCEPTED);

		when(supplierRepository.findByEmail("owner@supplier.test")).thenReturn(Optional.of(owner));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));

		assertThatThrownBy(() -> supplierService.rejectMatch(9L, "owner@supplier.test"))
				.isInstanceOf(BusinessRuleException.class);

		verify(demandRepository, never()).save(any());
	}
}
