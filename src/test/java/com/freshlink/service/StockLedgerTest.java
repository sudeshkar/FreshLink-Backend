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
import com.freshlink.enums.SupplyStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.DailySupply;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;
import com.freshlink.supplydto.DailySupplyCreateRequest;
import com.freshlink.supplydto.DailySupplyUpdateRequest;

/**
 * Fish.availableKg is the single stock ledger and a DailySupply row is the
 * dated intake behind it. They used to be independent numbers, so the spot
 * market and the matching engine could each believe they held stock the other
 * had already sold.
 */
@ExtendWith(MockitoExtension.class)
class StockLedgerTest {

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
		Supplier s = new Supplier();
		s.setId(1L);
		s.setEmail(EMAIL);
		return s;
	}

	private FishType tuna() {
		FishType t = new FishType();
		t.setId(1L);
		t.setName("Tuna");
		return t;
	}

	private Fish listing(Supplier owner, double available) {
		Fish f = new Fish();
		f.setId(50L);
		f.setSupplier(owner);
		f.setFishType(tuna());
		f.setName("Fresh Tuna");
		f.setPricePerKg(BigDecimal.valueOf(1800));
		f.setAvailableKg(available);
		f.setReservedKg(0);
		return f;
	}

	private DailySupply supply(Supplier owner, double quantity) {
		DailySupply d = new DailySupply();
		d.setId(7L);
		d.setSupplier(owner);
		d.setFishType(tuna());
		d.setQuantity(quantity);
		d.setStatus(SupplyStatus.AVAILABLE);
		d.setFreshnessScore(0.9);
		d.setCatchDateTime(LocalDateTime.now().minusHours(2));
		return d;
	}

	@Test
	@DisplayName("recording a catch adds it to the listing, so landed fish becomes sellable")
	void catchCreditsTheListing() {
		Supplier owner = supplier();
		Fish listing = listing(owner, 20.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishTypeRepository.findByNameIgnoreCase("Tuna")).thenReturn(Optional.of(tuna()));
		when(fishRepository.findBySupplierAndFishType(owner, tuna())).thenReturn(Optional.of(listing));
		when(dailySupplyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.addDailySupply(
				new DailySupplyCreateRequest("Tuna", 50.0, LocalDateTime.now().minusHours(1), 0.95),
				EMAIL);

		assertThat(listing.getAvailableKg())
				.as("20kg on the shelf plus a 50kg landing")
				.isEqualTo(70.0);
	}

	@Test
	@DisplayName("a catch with no listing to credit is refused with an actionable message")
	void catchWithoutListingIsRefused() {
		Supplier owner = supplier();

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishTypeRepository.findByNameIgnoreCase("Tuna")).thenReturn(Optional.of(tuna()));
		when(fishRepository.findBySupplierAndFishType(owner, tuna())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> supplierService.addDailySupply(
				new DailySupplyCreateRequest("Tuna", 50.0, LocalDateTime.now().minusHours(1), 0.9), EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Create a Tuna listing");

		verify(dailySupplyRepository, never()).save(any());
	}

	@Test
	@DisplayName("correcting a catch upward moves the ledger with it")
	void increasingACatchCreditsTheDifference() {
		Supplier owner = supplier();
		Fish listing = listing(owner, 50.0);
		DailySupply supply = supply(owner, 50.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));
		when(supplyMatchRepository.findByDailySupply(supply)).thenReturn(List.of());
		when(fishRepository.findBySupplierAndFishType(owner, supply.getFishType()))
				.thenReturn(Optional.of(listing));
		when(dailySupplyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateDailySupply(7L, new DailySupplyUpdateRequest(80.0, null), EMAIL);

		assertThat(listing.getAvailableKg()).as("only the 30kg difference is added").isEqualTo(80.0);
		assertThat(supply.getQuantity()).isEqualTo(80.0);
	}

	@Test
	@DisplayName("a catch cannot be corrected down below what has already been sold")
	void cannotCorrectBelowSoldStock() {
		Supplier owner = supplier();
		// 50kg landed, 45kg already sold, so only 5kg remains on the shelf.
		Fish listing = listing(owner, 5.0);
		DailySupply supply = supply(owner, 50.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));
		when(supplyMatchRepository.findByDailySupply(supply)).thenReturn(List.of());
		when(fishRepository.findBySupplierAndFishType(owner, supply.getFishType()))
				.thenReturn(Optional.of(listing));

		assertThatThrownBy(() -> supplierService.updateDailySupply(
				7L, new DailySupplyUpdateRequest(10.0, null), EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("below zero");

		assertThat(listing.getAvailableKg()).isEqualTo(5.0);
	}

	@Test
	@DisplayName("removing a catch takes its quantity back off the ledger")
	void deletingACatchDebitsTheListing() {
		Supplier owner = supplier();
		Fish listing = listing(owner, 70.0);
		DailySupply supply = supply(owner, 50.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));
		when(supplyMatchRepository.findByDailySupply(supply)).thenReturn(List.of());
		when(fishRepository.findBySupplierAndFishType(owner, supply.getFishType()))
				.thenReturn(Optional.of(listing));

		supplierService.deleteDailySupply(7L, EMAIL);

		assertThat(listing.getAvailableKg()).isEqualTo(20.0);
		verify(dailySupplyRepository).delete(supply);
	}

	@Test
	@DisplayName("a catch whose fish has been sold cannot be removed")
	void cannotDeleteASoldCatch() {
		Supplier owner = supplier();
		Fish listing = listing(owner, 10.0);
		DailySupply supply = supply(owner, 50.0);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(dailySupplyRepository.findById(7L)).thenReturn(Optional.of(supply));
		when(supplyMatchRepository.findByDailySupply(supply)).thenReturn(List.of());
		when(fishRepository.findBySupplierAndFishType(owner, supply.getFishType()))
				.thenReturn(Optional.of(listing));

		assertThatThrownBy(() -> supplierService.deleteDailySupply(7L, EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot be taken back");

		verify(dailySupplyRepository, never()).delete(any());
		assertThat(listing.getAvailableKg()).isEqualTo(10.0);
	}
}
