package com.freshlink.analytics.dto;

public record SupplierPerformanceDTO(
		Long supplierId,
        String supplierName,
        long ordersCompleted,
        double totalKgSupplied,
        double averageRating
		) {

}
