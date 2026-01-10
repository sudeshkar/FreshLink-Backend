package com.freshlink.fishdto;

public record FishMarketResponse(
		 	Long id,
	        String name,
	        String type,
	        double pricePerKg,
	        double availableKg,
	        String season,
	        String supplierName
		) {

}
