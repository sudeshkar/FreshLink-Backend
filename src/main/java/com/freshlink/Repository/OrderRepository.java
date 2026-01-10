package com.freshlink.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Cafe;
import com.freshlink.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long>{
	
	List<Order> findByCafe(Cafe cafe);

}
