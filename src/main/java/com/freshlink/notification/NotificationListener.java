package com.freshlink.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.freshlink.notification.NotificationEvents.DeliveryStatusChanged;
import com.freshlink.notification.NotificationEvents.OrderPlaced;
import com.freshlink.notification.NotificationEvents.OrderStatusChanged;
import com.freshlink.service.interfaces.EmailService;

import lombok.RequiredArgsConstructor;

/**
 * Sends transactional email in response to domain events.
 *
 * Two rules govern everything here:
 *
 * AFTER_COMMIT — an order that is rolled back must never generate an email
 * telling a supplier it exists. Listening on the commit phase means the work is
 * only announced once it is durable.
 *
 * Failures never propagate. Notification is a courtesy, not part of the
 * transaction: a dead SMTP server must not stop a cafe placing an order, and by
 * this point the transaction has committed anyway, so throwing would achieve
 * nothing but a stack trace on a background thread.
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

	private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

	private final EmailService emailService;

	@Value("${app.notifications.enabled:true}")
	private boolean enabled;

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderPlaced(OrderPlaced event) {
		send(event.supplierEmail(),
				"New order #%d from %s".formatted(event.orderId(), event.cafeName()),
				"""
						Hello %s,

						%s has placed order #%d with you, totalling LKR %.2f.

						Sign in to FreshLink to accept or reject it.
						""".formatted(event.supplierName(), event.cafeName(),
						event.orderId(), event.totalAmount()));
	}

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderStatusChanged(OrderStatusChanged event) {
		send(event.cafeEmail(),
				"Order #%d is now %s".formatted(event.orderId(), event.status()),
				"""
						Hello %s,

						Your order #%d with %s is now %s.
						""".formatted(event.cafeName(), event.orderId(),
						event.supplierName(), event.status()));
	}

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDeliveryStatusChanged(DeliveryStatusChanged event) {
		String driver = event.driverName() == null
				? "A driver has not been assigned yet."
				: "Driver: %s (%s)".formatted(event.driverName(),
						event.driverPhone() == null ? "no number given" : event.driverPhone());

		send(event.cafeEmail(),
				"Delivery update for order #%d".formatted(event.orderId()),
				"""
						Your delivery for order #%d is now %s.

						%s
						""".formatted(event.orderId(), event.status(), driver));
	}

	private void send(String to, String subject, String body) {
		if (!enabled) {
			log.debug("Notifications disabled - would have emailed {} about '{}'", to, subject);
			return;
		}

		try {
			emailService.sendEmail(to, subject, body);
		} catch (Exception e) {
			// Swallowed deliberately - see the class comment.
			log.warn("Could not send notification '{}' to {}: {}", subject, to, e.getMessage());
		}
	}
}
