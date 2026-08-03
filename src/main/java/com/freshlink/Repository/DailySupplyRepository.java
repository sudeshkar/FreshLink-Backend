package com.freshlink.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.freshlink.enums.SupplyStatus;
import com.freshlink.model.DailySupply;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;

public interface DailySupplyRepository extends JpaRepository<DailySupply, Long>{

	@Query("""
	        SELECT ds
	        FROM DailySupply ds
	        WHERE ds.fishType = :fishType
	          AND ds.status = :status
	          AND ds.quantity > 0
	          AND DATE(ds.catchDateTime) <= :requiredDate
	          AND ds.supplier.deletedAt IS NULL
	          AND ds.supplier.active = true
	    """)
	    List<DailySupply> findAvailableSupplies(
	            @Param("fishType") FishType fishType,
	            @Param("requiredDate") LocalDate requiredDate,
	            @Param("status") SupplyStatus status
	    );
	
	long countByStatus(SupplyStatus status);

	List<DailySupply> findBySupplierOrderByCatchDateTimeDesc(Supplier supplier);

	Page<DailySupply> findBySupplierOrderByCatchDateTimeDesc(Supplier supplier, Pageable pageable);

}
