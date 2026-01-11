package com.freshlink.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Order;
import com.freshlink.model.Rating;
import com.freshlink.model.Supplier;

public interface RatingRepository extends JpaRepository<Rating, Long>{

	boolean existsByOrder(Order order);

	List<Rating> findBySupplier(Supplier supplier);

}
