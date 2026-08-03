package com.freshlink.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An append-only record of what a listing has cost over time.
 *
 * Fish.pricePerKg is overwritten in place, so without this the previous price
 * is simply gone - and the price a supplier charged last week is some of the
 * most useful data the platform holds, both for cafes judging an offer and for
 * spotting seasonal movement.
 */
@Entity
@Table(name = "fish_price_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FishPriceHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fish_id", nullable = false)
	private Fish fish;

	@Column(nullable = false)
	private BigDecimal pricePerKg;

	@CreationTimestamp
	private LocalDateTime recordedAt;
}
