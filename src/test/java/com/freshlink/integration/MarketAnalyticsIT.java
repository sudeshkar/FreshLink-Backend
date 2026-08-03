package com.freshlink.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.marketdto.MarketPriceSummary;
import com.freshlink.marketdto.PriceTrend;
import com.freshlink.model.Supplier;
import com.freshlink.service.interfaces.MarketService;
import com.freshlink.service.interfaces.SupplierService;

/**
 * Market analytics are JPQL aggregates, so only a real query proves what they
 * count. The revenue figures on this project were wrong for exactly this
 * reason - a mocked repository will happily agree with whatever the code
 * expects.
 */
@SpringBootTest(properties = {
		"app.ratelimit.enabled=false",
		"app.notifications.enabled=false"
})
@ActiveProfiles("dev")
class MarketAnalyticsIT {

	@Autowired private MarketService marketService;
	@Autowired private SupplierService supplierService;
	@Autowired private SupplierRepository supplierRepository;
	@Autowired private FishTypeRepository fishTypeRepository;

	private static final String SUPPLIER_ONE = "supplier1@freshlink.com";

	private MarketPriceSummary summaryFor(String fishTypeName) {
		return marketService.getPriceSummary().stream()
				.filter(s -> s.fishTypeName().equalsIgnoreCase(fishTypeName))
				.findFirst()
				.orElseThrow(() -> new AssertionError(fishTypeName + " missing from the summary"));
	}

	@Test
	@DisplayName("the summary reports a real spread across live listings")
	void summaryReportsSpread() {
		List<MarketPriceSummary> summary = marketService.getPriceSummary();

		assertThat(summary).isNotEmpty();

		for (MarketPriceSummary row : summary) {
			assertThat(row.listingCount()).isPositive();
			assertThat(row.lowestPricePerKg())
					.as("%s: lowest must not exceed highest", row.fishTypeName())
					.isLessThanOrEqualTo(row.highestPricePerKg());
			assertThat(row.averagePricePerKg())
					.as("%s: average must sit within the spread", row.fishTypeName())
					.isBetween(row.lowestPricePerKg(), row.highestPricePerKg());
			assertThat(row.totalAvailableKg()).isNotNegative();
		}
	}

	@Test
	@DisplayName("a price change moves the market average")
	void priceChangeMovesTheAverage() {
		Supplier supplier = supplierRepository.findByEmail(SUPPLIER_ONE).orElseThrow();
		var listing = supplierService.getMyFish(SUPPLIER_ONE,
				org.springframework.data.domain.PageRequest.of(0, 50))
				.getContent().get(0);

		MarketPriceSummary before = summaryFor(listing.fishType());

		// Move this supplier's price well clear of wherever it was.
		BigDecimal raised = before.highestPricePerKg().add(BigDecimal.valueOf(500));
		supplierService.updateFish(listing.id(),
				new FishUpdateRequest(null, null, raised, null), SUPPLIER_ONE);

		MarketPriceSummary after = summaryFor(listing.fishType());

		assertThat(after.highestPricePerKg())
				.as("the raised price is now the top of the market")
				.isEqualByComparingTo(raised.setScale(2));
		assertThat(after.averagePricePerKg())
				.as("and the average moved with it")
				.isGreaterThan(before.averagePricePerKg());
		assertThat(supplier).isNotNull();
	}

	@Test
	@DisplayName("the trend records a point for the day a price changed")
	void trendCapturesAChange() {
		var listing = supplierService.getMyFish(SUPPLIER_ONE,
				org.springframework.data.domain.PageRequest.of(0, 50))
				.getContent().get(0);

		Long fishTypeId = fishTypeRepository.findByNameIgnoreCase(listing.fishType()).orElseThrow().getId();

		supplierService.updateFish(listing.id(),
				new FishUpdateRequest(null, null, new BigDecimal("2750.00"), null), SUPPLIER_ONE);

		PriceTrend trend = marketService.getPriceTrend(fishTypeId, 30);

		assertThat(trend.fishTypeId()).isEqualTo(fishTypeId);
		assertThat(trend.windowDays()).isEqualTo(30);
		assertThat(trend.points()).isNotEmpty();
		assertThat(trend.points().get(trend.points().size() - 1).date())
				.isEqualTo(java.time.LocalDate.now());
		assertThat(trend.periodHigh()).isGreaterThanOrEqualTo(trend.periodLow());
	}

	@Test
	@DisplayName("an unknown fish type is a 404, not an empty chart")
	void unknownFishTypeIsNotFound() {
		assertThatThrownBy(() -> marketService.getPriceTrend(999_999L, 30))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("an absurd window is refused rather than scanning everything")
	void windowIsBounded() {
		Long anyType = fishTypeRepository.findAll().get(0).getId();

		assertThatThrownBy(() -> marketService.getPriceTrend(anyType, 5000))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> marketService.getPriceTrend(anyType, 0))
				.isInstanceOf(BusinessRuleException.class);
	}
}
