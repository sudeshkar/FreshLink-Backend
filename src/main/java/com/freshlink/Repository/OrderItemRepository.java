package com.freshlink.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{

}
