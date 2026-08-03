package com.freshlink.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{

	Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByEmail(String email);

    /** Housekeeping: rotated and expired rows are kept only long enough to catch replay. */
    void deleteByExpiryTimeBefore(LocalDateTime cutoff);
}
