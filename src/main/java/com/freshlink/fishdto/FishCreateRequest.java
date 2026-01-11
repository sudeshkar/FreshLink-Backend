package com.freshlink.fishdto;

import java.math.BigDecimal;

public record FishCreateRequest(
		String name,
		String fishTypeName,
		BigDecimal  pricePerKg,
        double availableKg
		) {

}
