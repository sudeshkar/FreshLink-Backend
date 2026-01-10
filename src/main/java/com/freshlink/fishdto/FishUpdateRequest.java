package com.freshlink.fishdto;

public record FishUpdateRequest(
			String name,
	        String fishTypeName,
	        Double pricePerKg,
	        Double availableKg
		) {

}
