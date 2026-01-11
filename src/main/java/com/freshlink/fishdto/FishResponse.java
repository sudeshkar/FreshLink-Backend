package com.freshlink.fishdto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FishResponse(
		Long id,
        String name,
        String fishType,
        BigDecimal pricePerKg,
        double availableKg,
        boolean inSeason,
        LocalDate seasonStart,
        LocalDate seasonEnd,
        String supplierName,
        String supplierEmail
		) {

}
