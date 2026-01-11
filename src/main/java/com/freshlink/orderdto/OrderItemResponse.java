package com.freshlink.orderdto;

import java.math.BigDecimal;

public record OrderItemResponse(
		String fishName,
        double quantityKg,
        BigDecimal pricePerKg,
        double totalPrice
		) {

}
