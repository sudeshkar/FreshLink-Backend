package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.OtpVerification;

public interface OtpRepository extends JpaRepository<OtpVerification, Long>{
	Optional<OtpVerification> findByEmailAndOtp(String email, String otp);
    Optional<OtpVerification> findTopByEmailOrderByExpiresAtDesc(String email);

}
