package com.freshlink.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.freshlink.enums.DemandStatus;
import com.freshlink.model.Cafe;
import com.freshlink.model.DemandRequest;

public interface DemandRepository extends JpaRepository<DemandRequest, Long>{

	List<DemandRequest> findByStatusIn(List<DemandStatus> statuses);

	/** Demand still worth matching: unfilled, and not already past its delivery date. */
	List<DemandRequest> findByStatusInAndRequiredDateGreaterThanEqual(
			List<DemandStatus> statuses, LocalDate from);

	/** Unfilled demand whose delivery date has passed and can never be met. */
	List<DemandRequest> findByStatusInAndRequiredDateBefore(
			List<DemandStatus> statuses, LocalDate before);
	long countByStatus(DemandStatus status);

	/** Demand belonging to one cafe. Never expose another cafe's demand to it. */
	Page<DemandRequest> findByCafe(Cafe cafe, Pageable pageable);

	@Query("""
	    SELECT d.fishType.name, SUM(d.requestedQuantity)
	    FROM DemandRequest d
	    GROUP BY d.fishType.name
	    ORDER BY SUM(d.requestedQuantity) DESC
	""")
	List<Object[]> findTopFishDemand();



}
