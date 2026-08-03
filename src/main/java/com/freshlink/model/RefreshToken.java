package com.freshlink.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * SHA-256 of the token, never the token itself, so a database dump yields no
	 * usable credentials. A plain hash is enough here because the token is 256
	 * bits of randomness rather than a guessable secret.
	 */
	@Column(nullable = false, unique = true)
	private String tokenHash;

	private String email;

	private LocalDateTime expiryTime;

	/**
	 * Set when the token is exchanged. Rotated tokens are kept rather than
	 * deleted so a second presentation can be recognised as a replay.
	 */
	private boolean used;

	@CreationTimestamp
	private LocalDateTime createdAt;
}
