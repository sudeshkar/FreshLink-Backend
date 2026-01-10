package com.freshlink.service.interfaces;

import com.freshlink.model.RefreshToken;

public interface RefreshTokenService {
	RefreshToken createToken(String email);
    String refreshAccessToken(String refreshToken);
    void deleteByEmail(String email);
}
