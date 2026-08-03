package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.freshlink.Repository.DeliveryRouteRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishPriceHistoryRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.MatchStatus;
import com.freshlink.enums.OrderStatus;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.Cafe;
import com.freshlink.model.DailySupply;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Order;
import com.freshlink.model.Supplier;
import com.freshlink.model.SupplyMatch;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;

/**
 * Accepting a demand match creates a real order against a Fish listing, so the
 * listing has to be reserved exactly as a spot-market order would reserve it.
 *
 * It previously was not: the order was created without touching the listing, so
 * the same kilos stayed on sale after being promised to a café, and accepting
 * the resulting order decremented a reservation that had never been made -
 * driving reservedKg negative.
 */
@ExtendWith(MockitoExtension.class)
class MatchedOrderStockTest {

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
	@Mock private DemandMatchService demandMatchService;
	@Mock private FishPriceHistoryRepository fishPriceHistoryRepository;
	@Mock private ApplicationEventPublisher events;
	@Mock private DeliveryRouteRepository deliveryRouteRepository;

	@InjectMocks private SupplierServiceImpl supplierService;

	private static final String EMAIL = "owner@supplier.test";

	private Supplier supplier() {
		Supplier supplier = new Supplier();
		supplier.setId(1L);
		supplier.setEmail(EMAIL);
		return supplier;
	}

	private FishType tuna() {
		FishType type = new FishType();
		type.setId(1L);
		type.setName("Tuna");
		return type;
	}

	private Fish listing(Supplier owner, double available) {
		Fish fish = new Fish();
		fish.setId(50L);
		fish.setSupplier(owner);
		fish.setFishType(tuna());
		fish.setName("Fresh Tuna");
		fish.setPricePerKg(BigDecimal.valueOf(1800));
		fish.setAvailableKg(available);
		fish.setReservedKg(0);
		return fish;
	}

	private SupplyMatch pendingMatch(Supplier owner, double quantity) {
		Cafe cafe = new Cafe();
		cafe.setId(9L);

		DailySupply supply = new DailySupply();
		supply.setId(3L);
		supply.setSupplier(owner);
		supply.setFishType(tuna());
		supply.setQuantity(quantity + 10);
		supply.setStatus(SupplyStatus.AVAILABLE);

		DemandRequest demand = new DemandRequest();
		demand.setId(20L);
		demand.setCafe(cafe);
		demand.setFishType(tuna());

		SupplyMatch match = new SupplyMatch();
		match.setId(9L);
		match.setDailySupply(supply);
		match.setDemandRequest(demand);
		match.setConfirmedQuantity(quantity);
		match.setStatus(MatchStatus.PENDING);
		match.setCreatedAt(LocalDateTime.now());
		return match;
	}

	@Test
	@DisplayName("accepting a match reserves the listing, so the kilos leave the market")
	void acceptingAMatchReservesStock() {
		Supplier owner = supplier();
		Fish fish = listing(owner, 100.0);
		SupplyMatch match = pendingMatch(owner, 30.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));
		when(fishRepository.findBySupplierAndFishType(owner, match.getDemandRequest().getFishType()))
				.thenReturn(Optional.of(fish));
		when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.acceptMatch(9L, EMAIL);

		assertThat(fish.getAvailableKg())
				.as("30kg promised to a cafe must not still be on sale")
				.isEqualTo(70.0);
		assertThat(fish.getReservedKg()).isEqualTo(30.0);
	}

	@Test
	@DisplayName("the reservation survives the resulting order being accepted, without going negative")
	void reservationSurvivesOrderAcceptance() {
		Supplier owner = supplier();
		Fish fish = listing(owner, 100.0);
		SupplyMatch match = pendingMatch(owner, 30.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));
		when(fishRepository.findBySupplierAndFishType(owner, match.getDemandRequest().getFishType()))
				.thenReturn(Optional.of(fish));

		Order[] created = new Order[1];
		when(orderRepository.save(any())).thenAnswer(inv -> {
			Order o = inv.getArgument(0);
			o.setId(77L);
			created[0] = o;
			return o;
		});

		supplierService.acceptMatch(9L, EMAIL);

		// Now the supplier accepts the order the match produced.
		when(orderRepository.findById(77L)).thenReturn(Optional.of(created[0]));
		supplierService.acceptOrder(77L, EMAIL);

		assertThat(fish.getReservedKg())
				.as("reserved must return to zero, not go negative")
				.isEqualTo(0.0);
		assertThat(fish.getAvailableKg())
				.as("the stock is sold, so it stays gone")
				.isEqualTo(70.0);
	}

	@Test
	@DisplayName("a match cannot be accepted when the listing lacks the stock")
	void cannotAcceptWithoutStock() {
		Supplier owner = supplier();
		Fish fish = listing(owner, 5.0);
		SupplyMatch match = pendingMatch(owner, 30.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));
		when(fishRepository.findBySupplierAndFishType(owner, match.getDemandRequest().getFishType()))
				.thenReturn(Optional.of(fish));

		assertThatThrownBy(() -> supplierService.acceptMatch(9L, EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("only 5.0 kg available");

		assertThat(fish.getAvailableKg()).isEqualTo(5.0);
		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("a supplier with no matching listing gets told what to do, not a 500")
	void missingListingIsABusinessError() {
		Supplier owner = supplier();
		SupplyMatch match = pendingMatch(owner, 30.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));
		when(fishRepository.findBySupplierAndFishType(owner, match.getDemandRequest().getFishType()))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> supplierService.acceptMatch(9L, EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("no Tuna listing");
	}

	@Test
	@DisplayName("the order created from a match points back at it")
	void matchedOrderIsTraceable() {
		Supplier owner = supplier();
		Fish fish = listing(owner, 100.0);
		SupplyMatch match = pendingMatch(owner, 30.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(supplyMatchRepository.findById(9L)).thenReturn(Optional.of(match));
		when(fishRepository.findBySupplierAndFishType(owner, match.getDemandRequest().getFishType()))
				.thenReturn(Optional.of(fish));

		Order[] created = new Order[1];
		when(orderRepository.save(any())).thenAnswer(inv -> {
			created[0] = inv.getArgument(0);
			return created[0];
		});

		supplierService.acceptMatch(9L, EMAIL);

		assertThat(created[0].getSupplyMatch()).isSameAs(match);
		assertThat(created[0].getStatus()).isEqualTo(OrderStatus.REQUESTED);
		assertThat(created[0].getItems()).hasSize(1);
		assertThat(created[0].getItems().get(0).getQuantityKg()).isEqualTo(30.0);
	}

}
