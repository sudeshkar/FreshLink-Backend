package com.freshlink.fishdto;

public record FishCreateRequest(
		String name,
		String fishTypeName,
        double pricePerKg,
        double availableKg
		) {

}
