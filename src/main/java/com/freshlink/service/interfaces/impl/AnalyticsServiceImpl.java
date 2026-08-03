package com.freshlink.service.interfaces.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.analytics.dto.AnalyticsResponse;
import com.freshlink.analytics.dto.CafeAnalyticsDTO;
import com.freshlink.analytics.dto.SupplierPerformanceDTO;
import com.freshlink.enums.DemandStatus;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.service.interfaces.AnalyticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService{
	
	private final OrderRepository orderRepository;
    private final DemandRepository demandRepository;
    private final DailySupplyRepository dailySupplyRepository;
	
    @Cacheable("admin-dashboard")
	@Override
	public AnalyticsResponse getAdminDashboardAnalytics() {
		long totalOrders = orderRepository.count();

        BigDecimal totalRevenue = orderRepository
                .sumTotalOrderValue()
                .orElse(BigDecimal.ZERO);
        
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayOrders = orderRepository.countOrdersFrom(todayStart);
        BigDecimal todayRevenue =orderRepository.sumRevenueFrom(todayStart);

        long openDemands = demandRepository.countByStatus(DemandStatus.OPEN);
        long matchedDemands = demandRepository.countByStatus(DemandStatus.FULLY_MATCHED);

        long availableSupplies = dailySupplyRepository
                .countByStatus(SupplyStatus.AVAILABLE);

        Map<String, Double> topFishDemand =
                demandRepository.findTopFishDemand()
                        .stream()
                        .collect(Collectors.toMap(
                                r -> (String) r[0],
                                r -> (Double) r[1]
                        ));

        return new AnalyticsResponse(
                totalOrders,
                totalRevenue,
                todayOrders,
                todayRevenue,
                openDemands,
                matchedDemands,
                availableSupplies,
                topFishDemand
        );
    }

	@Override
	public List<SupplierPerformanceDTO> getSupplierLeaderboard() {
		 return orderRepository.getSupplierPerformance();
	}

	@Override
	public List<CafeAnalyticsDTO> getChurnRiskCafes() {
		return orderRepository.getCafeAnalytics()
	            .stream()
	            .filter(c ->
	                c.lastOrderDate()
	                 .isBefore(LocalDateTime.now().minusDays(14))
	            )
	            .toList();
	} 
	

}
