package com.freshlink.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.analytics.dto.AnalyticsResponse;
import com.freshlink.analytics.dto.SupplierPerformanceDTO;
import com.freshlink.service.interfaces.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {
	
	private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public AnalyticsResponse getDashboard() {
    	
        return analyticsService.getAdminDashboardAnalytics();
    }
    
    @GetMapping("/suppliers")
    public List<SupplierPerformanceDTO> supplierLeaderboard() {
        return analyticsService.getSupplierLeaderboard();
    }

}
