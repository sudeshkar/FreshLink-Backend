package com.freshlink.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.enums.MatchStatus;
import com.freshlink.model.DailySupply;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.Supplier;
import com.freshlink.model.SupplyMatch;

public interface SupplyMatchRepository extends JpaRepository<SupplyMatch, Long>{
	List<SupplyMatch> findByDailySupply_SupplierAndStatus(Supplier supplier,MatchStatus status);

	List<SupplyMatch> findByDemandRequest(DemandRequest demandRequest);

	List<SupplyMatch> findByDailySupply(DailySupply dailySupply);

}
