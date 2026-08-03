package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishPriceHistoryRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.Fish;
import com.freshlink.model.FishPriceHistory;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;

@ExtendWith(MockitoExtension.class)
class PriceHistoryTest {

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

	private Fish fish(Supplier owner, String price) {
		Fish fish = new Fish();
		fish.setId(50L);
		fish.setSupplier(owner);
		fish.setFishType(tuna());
		fish.setName("Fresh Tuna");
		fish.setPricePerKg(new BigDecimal(price));
		return fish;
	}

	@Test
	@DisplayName("creating a listing records its opening price")
	void createRecordsOpeningPrice() {
		Supplier owner = supplier();
		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishTypeRepository.findByNameIgnoreCase("Tuna")).thenReturn(Optional.of(tuna()));
		when(fishRepository.existsBySupplierAndFishType(any(), any())).thenReturn(false);
		when(fishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.addFish(
				new FishCreateRequest("Fresh Tuna", "Tuna", new BigDecimal("1800.00"), 100),
				EMAIL);

		ArgumentCaptor<FishPriceHistory> captor = ArgumentCaptor.forClass(FishPriceHistory.class);
		verify(fishPriceHistoryRepository).save(captor.capture());
		assertThat(captor.getValue().getPricePerKg()).isEqualByComparingTo("1800.00");
	}

	@Test
	@DisplayName("changing the price records the new one")
	void priceChangeIsRecorded() {
		Supplier owner = supplier();
		Fish existing = fish(owner, "1800.00");

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishRepository.findById(50L)).thenReturn(Optional.of(existing));
		when(fishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateFish(50L,
				new FishUpdateRequest(null, null, new BigDecimal("2000.00"), null),
				EMAIL);

		ArgumentCaptor<FishPriceHistory> captor = ArgumentCaptor.forClass(FishPriceHistory.class);
		verify(fishPriceHistoryRepository).save(captor.capture());
		assertThat(captor.getValue().getPricePerKg()).isEqualByComparingTo("2000.00");
	}

	@Test
	@DisplayName("re-submitting the same price adds nothing, so the trend stays meaningful")
	void unchangedPriceIsNotRecorded() {
		Supplier owner = supplier();
		Fish existing = fish(owner, "1800.00");

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishRepository.findById(50L)).thenReturn(Optional.of(existing));
		when(fishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateFish(50L,
				new FishUpdateRequest(null, null, new BigDecimal("1800.00"), null),
				EMAIL);

		verify(fishPriceHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("the same value at a different scale is still the same price")
	void scaleDifferenceIsNotAPriceChange() {
		Supplier owner = supplier();
		Fish existing = fish(owner, "1800.00");

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishRepository.findById(50L)).thenReturn(Optional.of(existing));
		when(fishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		// compareTo rather than equals: BigDecimal("1800.0").equals(1800.00) is false.
		supplierService.updateFish(50L,
				new FishUpdateRequest(null, null, new BigDecimal("1800.0"), null),
				EMAIL);

		verify(fishPriceHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("editing something other than price records nothing")
	void nonPriceEditIsNotRecorded() {
		Supplier owner = supplier();
		Fish existing = fish(owner, "1800.00");

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishRepository.findById(50L)).thenReturn(Optional.of(existing));
		when(fishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateFish(50L,
				new FishUpdateRequest("Line-caught Tuna", null, null, null),
				EMAIL);

		verify(fishPriceHistoryRepository, never()).save(any());
		assertThat(existing.getName()).isEqualTo("Line-caught Tuna");
	}

	@Test
	@DisplayName("two successive changes leave two records")
	void successiveChangesAccumulate() {
		Supplier owner = supplier();
		Fish existing = fish(owner, "1800.00");

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(fishRepository.findById(50L)).thenReturn(Optional.of(existing));
		when(fishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateFish(50L,
				new FishUpdateRequest(null, null, new BigDecimal("1900.00"), null), EMAIL);
		supplierService.updateFish(50L,
				new FishUpdateRequest(null, null, new BigDecimal("2100.00"), null), EMAIL);

		verify(fishPriceHistoryRepository, times(2)).save(any());
	}
}
