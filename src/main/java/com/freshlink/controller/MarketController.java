package com.freshlink.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.marketdto.MarketPriceSummary;
import com.freshlink.marketdto.PriceTrend;
import com.freshlink.service.interfaces.MarketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Open to both trading roles on purpose: the same numbers answer "is this offer
 * fair" for a cafe and "am I priced competitively" for a supplier.
 */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CAFE','SUPPLIER')")
@Tag(name = "Market", description = "Market-wide pricing. Available to cafés and suppliers alike.")
public class MarketController {

	private final MarketService marketService;

	@Operation(summary = "Current price spread per fish type",
			description = "Lowest, highest and average price across every live listing, "
					+ "with how many suppliers are offering it and how much is available.")
	@GetMapping("/price-summary")
	public List<MarketPriceSummary> priceSummary() {
		return marketService.getPriceSummary();
	}

	@Operation(summary = "Price trend for one fish type",
			description = "Daily market average over the requested window. A day with no "
					+ "point means nobody changed their price that day.")
	@GetMapping("/fish-types/{fishTypeId}/price-trend")
	public PriceTrend priceTrend(
			@PathVariable Long fishTypeId,
			@RequestParam(defaultValue = "30") int days) {
		return marketService.getPriceTrend(fishTypeId, days);
	}
}
