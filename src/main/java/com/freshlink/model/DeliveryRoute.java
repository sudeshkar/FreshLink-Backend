package com.freshlink.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.freshlink.enums.RouteStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One driver, one trip, several drop-offs.
 *
 * Deliveries were previously managed one at a time, which does not match how a
 * supplier actually works: a van leaves with six cafés on board and the driver
 * details get retyped six times. A route assigns the driver once and moves every
 * stop together.
 */
@Entity
@Table(name = "delivery_route")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryRoute {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	/** The day the van goes out. */
	private LocalDate routeDate;

	private String driverName;

	private String driverPhone;

	@Enumerated(EnumType.STRING)
	private RouteStatus status;

	private String notes;

	@CreationTimestamp
	private LocalDateTime createdAt;

	/**
	 * No cascade delete: removing a route must never remove the deliveries on it.
	 * Deleting a route only unassigns its stops.
	 */
	@OneToMany(mappedBy = "route", cascade = CascadeType.PERSIST)
	private List<Delivery> deliveries = new ArrayList<>();
}
