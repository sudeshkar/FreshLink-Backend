package com.freshlink.orderdto;

import java.util.List;

public record OrderResponse(
			Long orderId,
	        String cafeName,
	        String cafeEmail,
	        List<OrderItemResponse> items,
	        double totalAmount,
	        String status,
	        String createdAt
		) {

}
