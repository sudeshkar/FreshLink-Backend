package com.freshlink.service.interfaces;

public interface OtpService {
	void sendOtp(String email);
    void verifyOtp(String email, String otp);
}
