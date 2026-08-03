package com.freshlink.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;

public interface FishRepository extends JpaRepository<Fish, Long>{
	 	List<Fish> findBySupplier(Supplier supplier);

	    // Find by FishType name
	    List<Fish> findByFishType_Name(String name);

	   
	    @Query("SELECT f FROM Fish f WHERE f.fishType.seasonStart <= :date AND f.fishType.seasonEnd >= :date")
	    List<Fish> findByCurrentSeason(@Param("date") LocalDate date);

	    /**
	     * Public market listing. Both filters are optional (pass null to skip), and
	     * suppliers that are deactivated or removed never appear.
	     */
	    @Query("""
	        SELECT f FROM Fish f
	        WHERE f.supplier.deletedAt IS NULL
	          AND f.supplier.active = true
	          AND (:fishType IS NULL OR LOWER(f.fishType.name) = LOWER(:fishType))
	          AND (:city IS NULL OR LOWER(f.supplier.location) = LOWER(:city))
	    """)
	    List<Fish> searchMarket(@Param("fishType") String fishType, @Param("city") String city);

		boolean existsBySupplierAndFishType(Supplier supplier, FishType fishType);

		Optional<Fish> findBySupplierAndFishType(Supplier supplier, FishType fishType);
}
