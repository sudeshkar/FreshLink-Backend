package com.freshlink.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.freshlink.model.Fish;
import com.freshlink.model.FishPriceHistory;

public interface FishPriceHistoryRepository extends JpaRepository<FishPriceHistory, Long>{

	Page<FishPriceHistory> findByFishOrderByRecordedAtDesc(Fish fish, Pageable pageable);

	/**
	 * Average price recorded per day for one fish type across every live supplier.
	 * Prices are only written when they actually change, so a day with no row
	 * means nobody moved their price - not that the fish was unpriced.
	 */
	@Query("""
	    SELECT DATE(h.recordedAt), AVG(h.pricePerKg), COUNT(h)
	    FROM FishPriceHistory h
	    WHERE h.fish.fishType.id = :fishTypeId
	      AND h.recordedAt >= :since
	      AND h.fish.supplier.deletedAt IS NULL
	      AND h.fish.supplier.active = true
	    GROUP BY DATE(h.recordedAt)
	    ORDER BY DATE(h.recordedAt)
	""")
	List<Object[]> findDailyAveragePrices(@Param("fishTypeId") Long fishTypeId,
			@Param("since") LocalDateTime since);
}
