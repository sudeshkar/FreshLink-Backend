package com.freshlink.analytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsResponse(
		long totalOrders,
        BigDecimal totalRevenue,
        
        long todayOrders,
        BigDecimal todayRevenue,
        
        long openDemands,
        long matchedDemands,
        
        long availableSupplies,
        Map<String, Double> topFishDemand
		) {

}
