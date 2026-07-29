package com.freshlink.service.interfaces.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.OtpRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.model.OtpVerification;
import com.freshlink.model.User;
import com.freshlink.service.interfaces.EmailService;
import com.freshlink.service.interfaces.OtpService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int MAX_ATTEMPTS = 3;
	private static final int VALIDITY_MINUTES = 5;

	private final OtpRepository otpRepository;
	private final EmailService emailService;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public void sendOtp(String email) {
		// Invalidate any previous codes so only the newest one is usable and
		// stale rows do not accumulate.
		otpRepository.deleteByEmail(email);

		String otp = generateOtp();

		OtpVerification entity = new OtpVerification();
		entity.setEmail(email);
		entity.setOtp(otp);
		entity.setExpiresAt(LocalDateTime.now().plusMinutes(VALIDITY_MINUTES));

		otpRepository.save(entity);

		String subject = "OTP Verification";
		String body = "Your OTP is: " + otp + "\nThis OTP is valid for " + VALIDITY_MINUTES + " minutes.";

		emailService.sendEmail(entity.getEmail(), subject, body);
	}

	@Override
	@Transactional
	public void verifyOtp(String email, String otp) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found"));

		// Look the record up by email only. Fetching by email *and* OTP would mean a
		// wrong guess matched no row at all, leaving the attempt counter unreachable.
		OtpVerification data = otpRepository.findTopByEmailOrderByExpiresAtDesc(email)
				.orElseThrow(() -> new RuntimeException("No OTP has been requested for this email"));

		if (data.isVerified()) {
			throw new RuntimeException("This OTP has already been used");
		}

		if (data.getAttempts() >= MAX_ATTEMPTS) {
			throw new RuntimeException("Too many incorrect attempts. Please request a new OTP.");
		}

		if (data.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("OTP expired");
		}

		if (!data.getOtp().equals(otp)) {
			data.setAttempts(data.getAttempts() + 1);
			otpRepository.save(data);
			throw new RuntimeException("Invalid OTP");
		}

		user.setEmailVerified(true);
		data.setVerified(true);
		userRepository.save(user);
		otpRepository.save(data);
	}

	private String generateOtp() {
		int otp = SECURE_RANDOM.nextInt(900000) + 100000;
		return String.valueOf(otp);
	}
}
