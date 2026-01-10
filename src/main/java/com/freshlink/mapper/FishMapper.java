package com.freshlink.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;

@Component
public class FishMapper {
	public FishResponse toFishResponse(Fish fish) {
        FishType type = fish.getFishType();

        boolean inSeason = false;
        if (type.getSeasonStart() != null && type.getSeasonEnd() != null) {
            LocalDate today = LocalDate.now();
            inSeason = !today.isBefore(type.getSeasonStart())
                    && !today.isAfter(type.getSeasonEnd());
        }
        
        return new FishResponse(
                fish.getId(),
                fish.getName(),
                type.getName(),
                fish.getPricePerKg(),
                fish.getAvailableKg(),
                inSeason,
                type.getSeasonStart(),
                type.getSeasonEnd(),
                fish.getSupplier().getName(),
                fish.getSupplier().getEmail()
            );
	}
	
	public FishMarketResponse toFishMarket(Fish fish) {

        FishType type = fish.getFishType();

        String season = "OUT_OF_SEASON";
        if (type.getSeasonStart() != null && type.getSeasonEnd() != null) {
            LocalDate today = LocalDate.now();
            if (!today.isBefore(type.getSeasonStart())
                    && !today.isAfter(type.getSeasonEnd())) {
                season = "IN_SEASON";
            }
        }

        return new FishMarketResponse(
            fish.getId(),
            fish.getName(),
            type.getName(),
            fish.getPricePerKg(),
            fish.getAvailableKg(),
            season,
            fish.getSupplier().getName()
        );
    }
}
