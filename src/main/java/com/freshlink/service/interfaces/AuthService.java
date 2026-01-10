package com.freshlink.service.interfaces;

import com.freshlink.authdto.AuthResponseDto;
import com.freshlink.authdto.CafeRegisterRequest;
import com.freshlink.authdto.LoginRequestDto;
import com.freshlink.authdto.RefreshTokenRequestDto;
import com.freshlink.authdto.SupplierRegisterRequest;

public interface AuthService {
		AuthResponseDto login(LoginRequestDto dto);
	    AuthResponseDto refresh(RefreshTokenRequestDto dto);
	    void logout(String email);
		void registerCafe(CafeRegisterRequest dto);
		void registerSupplier(SupplierRegisterRequest dto);
}
