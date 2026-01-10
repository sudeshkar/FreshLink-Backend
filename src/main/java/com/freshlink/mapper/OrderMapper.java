package com.freshlink.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.freshlink.model.Order;
import com.freshlink.model.OrderItem;
import com.freshlink.orderdto.OrderItemResponse;
import com.freshlink.orderdto.OrderResponse;

@Component
public class OrderMapper {
	
	public OrderResponse toOrderResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        double totalAmount = itemResponses.stream()
                .mapToDouble(OrderItemResponse::totalPrice)
                .sum();

        return new OrderResponse(
            order.getId(),
            order.getCafe().getName(),
            order.getCafe().getEmail(),
            itemResponses,
            totalAmount,
            order.getStatus().name(),
            order.getCreatedAt().toString()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {

        double totalPrice = item.getQuantityKg() * item.getPricePerKg();

        return new OrderItemResponse(
            item.getFish().getName(),
            item.getQuantityKg(),
            item.getPricePerKg(),
            totalPrice
        );
    }
}
