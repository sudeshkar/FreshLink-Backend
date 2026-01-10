package com.freshlink.mapper;

import org.springframework.stereotype.Component;

import com.freshlink.model.Cafe;
import com.freshlink.userprofiledto.CafeProfileResponse;

@Component
public class CafeMapper {
	
	public CafeProfileResponse toProfile(Cafe cafe) {
        return new CafeProfileResponse(
            cafe.getName(),
            cafe.getEmail(),
            cafe.getPhone(),
            cafe.isActive()
        );
    }
}
