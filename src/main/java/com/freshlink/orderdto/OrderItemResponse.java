package com.freshlink.orderdto;

public record OrderItemResponse(
		String fishName,
        double quantityKg,
        double pricePerKg,
        double totalPrice
		) {

}
