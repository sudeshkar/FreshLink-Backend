package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Delivery;
import com.freshlink.model.Order;

public interface DeliveryRepository extends JpaRepository<Delivery, Long>{

	Optional<Delivery> findByOrder(Order order);
}
