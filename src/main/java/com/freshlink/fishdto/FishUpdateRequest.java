package com.freshlink.fishdto;

import java.math.BigDecimal;

public record FishUpdateRequest(
			String name,
	        String fishTypeName,
	        BigDecimal pricePerKg,
	        Double availableKg
		) {

}
