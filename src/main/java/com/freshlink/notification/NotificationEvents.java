package com.freshlink.notification;

/**
 * Events the notification layer reacts to.
 *
 * They carry plain values rather than entities on purpose: notifications are
 * handled after the transaction commits and on another thread, where a lazy
 * proxy would have no session to initialise from.
 */
public final class NotificationEvents {

	private NotificationEvents() {
	}

	/** A cafe placed an order; the supplier needs to know it is waiting. */
	public record OrderPlaced(
			Long orderId,
			String supplierEmail,
			String supplierName,
			String cafeName,
			double totalAmount) {
	}

	/** The supplier responded, or moved the order along. */
	public record OrderStatusChanged(
			Long orderId,
			String cafeEmail,
			String cafeName,
			String supplierName,
			String status) {
	}

	/** A delivery moved - the cafe wants to know when to expect the driver. */
	public record DeliveryStatusChanged(
			Long orderId,
			String cafeEmail,
			String status,
			String driverName,
			String driverPhone) {
	}
}
