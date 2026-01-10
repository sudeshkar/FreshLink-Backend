package com.freshlink.fishdto;

import java.time.LocalDate;

public record FishResponse(
		Long id,
        String name,
        String fishType,
        double pricePerKg,
        double availableKg,
        boolean inSeason,
        LocalDate seasonStart,
        LocalDate seasonEnd,
        String supplierName,
        String supplierEmail
		) {

}
