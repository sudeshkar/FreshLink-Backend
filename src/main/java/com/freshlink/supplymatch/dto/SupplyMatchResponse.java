package com.freshlink.supplymatch.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.freshlink.enums.MatchStatus;

public record SupplyMatchResponse(
		 // Match info
        Long matchId,
        MatchStatus matchStatus,
        Double confirmedQuantity,

        // Demand side
        Long demandRequestId,
        String cafeName,
        String fishType,
        Double requestedQuantity,
        LocalDate requiredDate,

        // Supply side
        Long dailySupplyId,
        String supplierName,
        Double suppliedQuantity,
        LocalDateTime catchDateTime,
        Double freshnessScore
		) {

}
