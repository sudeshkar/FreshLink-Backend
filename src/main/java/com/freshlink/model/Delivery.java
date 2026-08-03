package com.freshlink.model;

import java.time.LocalDateTime;

import com.freshlink.enums.DeliveryStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long deliveryId;

	@OneToOne
	@JoinColumn(name = "order_id")
	private Order order;

	/** When the delivery record was raised. */
	private LocalDateTime deliveryDate;

	@Enumerated(EnumType.STRING)
	private DeliveryStatus status;

	private String driverName;

	private String driverPhone;

	/** What the cafe is told to expect, so they can staff the drop-off. */
	private LocalDateTime expectedAt;

	/** Set once the goods actually arrive; null until then. */
	private LocalDateTime deliveredAt;

	/** Free text from the driver - gate code, or why an attempt failed. */
	private String notes;

	/** Set when this drop-off is part of a driver's route. Null if standalone. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id")
	private DeliveryRoute route;
}
