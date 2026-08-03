package com.freshlink.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;

public interface FishRepository extends JpaRepository<Fish, Long>{
	 	List<Fish> findBySupplier(Supplier supplier);

	 	Page<Fish> findBySupplier(Supplier supplier, Pageable pageable);

	    // Find by FishType name
	    List<Fish> findByFishType_Name(String name);

	   
	    @Query("SELECT f FROM Fish f WHERE f.fishType.seasonStart <= :date AND f.fishType.seasonEnd >= :date")
	    List<Fish> findByCurrentSeason(@Param("date") LocalDate date);

	    /**
	     * Public market listing. Both filters are optional (pass null to skip), and
	     * suppliers that are deactivated or removed never appear.
	     *
	     * Callers must pass the filters already lower-cased. LOWER() is deliberately
	     * not applied to the parameters: with a null bind PostgreSQL cannot infer the
	     * type and resolves lower(bytea), which does not exist - so an unfiltered
	     * browse, the default view, fails outright.
	     */
	    @Query("""
	        SELECT f FROM Fish f
	        WHERE f.supplier.deletedAt IS NULL
	          AND f.supplier.active = true
	          AND (:fishType IS NULL OR LOWER(f.fishType.name) = :fishType)
	          AND (:city IS NULL OR LOWER(f.supplier.location) = :city)
	    """)
	    Page<Fish> searchMarket(@Param("fishType") String fishType, @Param("city") String city,
	            Pageable pageable);

		boolean existsBySupplierAndFishType(Supplier supplier, FishType fishType);

		Optional<Fish> findBySupplierAndFishType(Supplier supplier, FishType fishType);

	    /**
	     * Market-wide price spread per fish type. Excludes suspended and removed
	     * suppliers for the same reason browsing does: their prices are not
	     * obtainable, so including them would misreport what the market costs.
	     */
	    @Query("""
	        SELECT ft.id, ft.name,
	               MIN(f.pricePerKg), MAX(f.pricePerKg), AVG(f.pricePerKg),
	               COUNT(f), COALESCE(SUM(f.availableKg), 0),
	               ft.seasonStart, ft.seasonEnd
	        FROM Fish f
	        JOIN f.fishType ft
	        WHERE f.supplier.deletedAt IS NULL
	          AND f.supplier.active = true
	        GROUP BY ft.id, ft.name, ft.seasonStart, ft.seasonEnd
	        ORDER BY ft.name
	    """)
	    List<Object[]> findMarketPriceSummary();
}
