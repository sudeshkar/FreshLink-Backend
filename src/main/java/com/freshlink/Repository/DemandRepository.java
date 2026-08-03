package com.freshlink.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.freshlink.enums.DemandStatus;
import com.freshlink.model.Cafe;
import com.freshlink.model.DemandRequest;

public interface DemandRepository extends JpaRepository<DemandRequest, Long>{

	List<DemandRequest> findByStatusIn(List<DemandStatus> statuses);
	long countByStatus(DemandStatus status);

	/** Demand belonging to one cafe. Never expose another cafe's demand to it. */
	List<DemandRequest> findByCafe(Cafe cafe);

	@Query("""
	    SELECT d.fishType.name, SUM(d.requestedQuantity)
	    FROM DemandRequest d
	    GROUP BY d.fishType.name
	    ORDER BY SUM(d.requestedQuantity) DESC
	""")
	List<Object[]> findTopFishDemand();



}
