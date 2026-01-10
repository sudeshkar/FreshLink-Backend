package com.freshlink.authdto;

public record AuthResponseDto(
		String accessToken,
        String refreshToken,
        String role
        ) {

}
