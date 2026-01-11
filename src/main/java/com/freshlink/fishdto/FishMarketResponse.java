package com.freshlink.fishdto;

import java.math.BigDecimal;

public record FishMarketResponse(
		 	Long id,
	        String name,
	        String type,
	        BigDecimal pricePerKg,
	        double availableKg,
	        String season,
	        String supplierName
		) {

}
