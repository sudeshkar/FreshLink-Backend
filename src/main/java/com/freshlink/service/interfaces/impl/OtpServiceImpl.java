package com.freshlink.service.interfaces.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import com.freshlink.Repository.OtpRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.model.OtpVerification;
import com.freshlink.model.User;
import com.freshlink.service.interfaces.EmailService;
import com.freshlink.service.interfaces.OtpService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService{

	private final  OtpRepository otpRepository;
	private final EmailService emailService;
	private final UserRepository userRepository;
	private static final SecureRandom secureRandom = new SecureRandom();
	@Override
	public void sendOtp(String email) {
		String otp = generateOtp();

        OtpVerification entity = new OtpVerification();
        entity.setEmail(email);
        entity.setOtp(otp);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(entity);
        
        String subject = "OTP Verification";
        String body = "Your OTP is: " + otp + "\nThis OTP is valid for 5 minutes.";

        emailService.sendEmail(entity.getEmail(), subject, body);
		
	}

	@Override
	public void verifyOtp(String email, String otp) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(()->new RuntimeException("Supplier Not Found"));
		
		 OtpVerification data = otpRepository.findByEmailAndOtp(email, otp)
				 .orElseThrow(() -> new RuntimeException("Invalid OTP"));
		 
		 if (data.getAttempts() >= 3) {
			    throw new RuntimeException("Too many OTP requests. Try later.");
			}
		 
		 if (data.getExpiresAt().isBefore(LocalDateTime.now())) {
	            throw new RuntimeException("OTP expired");
	        }
		 user.setEmailVerified(true);
		 data.setVerified(true);
		 userRepository.save(user);
		 otpRepository.save(data);
	
		
	}
	
	private String generateOtp() {
	    int otp = secureRandom.nextInt(900000) + 100000;
	    return String.valueOf(otp);
	}

}
