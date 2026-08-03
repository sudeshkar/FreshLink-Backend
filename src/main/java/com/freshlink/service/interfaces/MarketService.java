package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.marketdto.MarketPriceSummary;
import com.freshlink.marketdto.PriceTrend;

public interface MarketService {

	/** What every fish type is going for across the market right now. */
	List<MarketPriceSummary> getPriceSummary();

	/** Daily average price for one fish type over the last {@code days} days. */
	PriceTrend getPriceTrend(Long fishTypeId, int days);
}
