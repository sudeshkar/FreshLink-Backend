package com.freshlink.authdto;

public record VerifyOtpRequest(
		String email,
	    String otp
		) {

}
