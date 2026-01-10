package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
	Optional<RefreshToken> findByToken(String token);

    void deleteByEmail(String email);
}
