package com.freshlink.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.freshlink.analytics.dto.CafeAnalyticsDTO;
import com.freshlink.analytics.dto.SupplierPerformanceDTO;
import com.freshlink.enums.OrderStatus;
import com.freshlink.model.Cafe;
import com.freshlink.model.Order;
import com.freshlink.model.Supplier;

public interface OrderRepository extends JpaRepository<Order, Long>{
	
	List<Order> findByCafe(Cafe cafe);

	Page<Order> findByCafe(Cafe cafe, Pageable pageable);

	List<Order> findBySupplier(Supplier supplier);

	Page<Order> findBySupplier(Supplier supplier, Pageable pageable);

	long countByCafeAndStatusIn(Cafe cafe, List<OrderStatus> statuses);

	long countBySupplierAndStatusIn(Supplier supplier, List<OrderStatus> statuses);
	
	/**
	 * Revenue means money actually earned, so only completed orders count.
	 * Without the filter, rejected and cancelled orders were reported as revenue -
	 * and the supplier leaderboard below already filtered on COMPLETED, so the
	 * dashboard contradicted itself.
	 */
	@Query("""
		    SELECT COALESCE(SUM(i.quantityKg * i.pricePerKg), 0)
		    FROM Order o JOIN o.items i
		    WHERE o.status = com.freshlink.enums.OrderStatus.COMPLETED
		""")
	Optional<BigDecimal> sumTotalOrderValue();
	
	@Query("""
		    SELECT COUNT(o)
		    FROM Order o
		    WHERE o.createdAt >= :start
		""")
		long countOrdersFrom(LocalDateTime start);
	
	@Query("""
		    SELECT COALESCE(SUM(i.quantityKg * i.pricePerKg), 0)
		    FROM Order o JOIN o.items i
		    WHERE o.createdAt >= :start
		      AND o.status = com.freshlink.enums.OrderStatus.COMPLETED
		""")
		BigDecimal sumRevenueFrom(LocalDateTime start);
	
	@Query("""
		    SELECT new com.freshlink.analytics.dto.SupplierPerformanceDTO(
		        s.id,
		        s.name,
		        COUNT(o),
		        SUM(i.quantityKg),
		        s.averageRating
		    )
		    FROM Order o
		    JOIN o.supplier s
		    JOIN o.items i
		    WHERE o.status = 'COMPLETED'
		    GROUP BY s.id, s.name, s.averageRating
		    ORDER BY SUM(i.quantityKg) DESC
		""")
		List<SupplierPerformanceDTO> getSupplierPerformance();

	@Query("""
		    SELECT new com.freshlink.analytics.dto.CafeAnalyticsDTO(
		        c.id,
		        c.name,
		        COUNT(o),
		        MAX(o.createdAt)
		    )
		    FROM Order o
		    JOIN o.cafe c
		    GROUP BY c.id, c.name
		""")
		List<CafeAnalyticsDTO> getCafeAnalytics();


}
