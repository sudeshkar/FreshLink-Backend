package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.analytics.dto.AnalyticsResponse;
import com.freshlink.analytics.dto.CafeAnalyticsDTO;
import com.freshlink.analytics.dto.SupplierPerformanceDTO;

public interface AnalyticsService {
	AnalyticsResponse getAdminDashboardAnalytics();
	
	List<SupplierPerformanceDTO> getSupplierLeaderboard();
	
	List<CafeAnalyticsDTO> getChurnRiskCafes();

}
