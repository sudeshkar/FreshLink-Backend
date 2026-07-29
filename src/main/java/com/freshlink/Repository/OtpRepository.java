package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.OtpVerification;

public interface OtpRepository extends JpaRepository<OtpVerification, Long>{
	Optional<OtpVerification> findTopByEmailOrderByExpiresAtDesc(String email);

	void deleteByEmail(String email);

}
