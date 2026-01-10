package com.freshlink.userprofiledto;

public record CafeProfileResponse(
		String name,
        String email,
        String phone,
        boolean active
		) {

}
