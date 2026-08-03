package com.freshlink.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Fish;
import com.freshlink.model.FishPriceHistory;

public interface FishPriceHistoryRepository extends JpaRepository<FishPriceHistory, Long>{

	Page<FishPriceHistory> findByFishOrderByRecordedAtDesc(Fish fish, Pageable pageable);
}
