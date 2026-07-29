package com.freshlink.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.authdto.AuthResponseDto;
import com.freshlink.authdto.CafeRegisterRequest;
import com.freshlink.authdto.LoginRequestDto;
import com.freshlink.authdto.RefreshTokenRequestDto;
import com.freshlink.authdto.SupplierRegisterRequest;
import com.freshlink.authdto.VerifyOtpRequest;
import com.freshlink.service.interfaces.AuthService;
import com.freshlink.service.interfaces.OtpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthService authService;
	private final OtpService otpService;
     
    
    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto dto) {
    	return authService.login(dto);
    }
    
    @PostMapping("/refresh")
    public AuthResponseDto refresh(@RequestBody RefreshTokenRequestDto dto) {

        return authService.refresh(dto);
    }
    
    @PostMapping("/logout")
    public String logout(Authentication auth) {

    	if (auth != null) {
            authService.logout(auth.getName());
        }
    	SecurityContextHolder.clearContext();
        return "Logged out successfully";
    }
    
    @PostMapping("/register/cafe")
    public void registerCafe(@RequestBody CafeRegisterRequest dto) {
        authService.registerCafe(dto);
    }

    @PostMapping("/register/supplier")
    public void registerSupplier(@RequestBody SupplierRegisterRequest dto) {
        authService.registerSupplier(dto);
    }
    
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody VerifyOtpRequest dto) {
    	
    	otpService.verifyOtp(dto.email(), dto.otp());
    	return "OTP Verified";
    	
    }
    
    @PostMapping("/request-otp")
	public String requestOtp(@RequestBody String email) {
	    otpService.sendOtp(email);
	    
	    return "OTP sent to email";
	}
    


}
