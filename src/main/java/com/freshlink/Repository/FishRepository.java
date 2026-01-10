package com.freshlink.Repository;

import java.time.LocalDate;
import java.util.List;

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

		boolean existsBySupplierAndFishType(Supplier supplier, FishType fishType);
}
