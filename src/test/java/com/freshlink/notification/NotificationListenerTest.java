package com.freshlink.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.freshlink.notification.NotificationEvents.DeliveryStatusChanged;
import com.freshlink.notification.NotificationEvents.OrderPlaced;
import com.freshlink.notification.NotificationEvents.OrderStatusChanged;
import com.freshlink.service.interfaces.EmailService;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

	@Mock private EmailService emailService;

	@InjectMocks private NotificationListener listener;

	@BeforeEach
	void enable() {
		ReflectionTestUtils.setField(listener, "enabled", true);
	}

	@Test
	@DisplayName("a new order emails the supplier, not the cafe")
	void orderPlacedNotifiesSupplier() {
		listener.onOrderPlaced(new OrderPlaced(42L, "supplier@test.com", "Ocean Fresh",
				"Cafe Blue Wave", 9000.0));

		ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(to.capture(), subject.capture(), body.capture());

		assertThat(to.getValue()).isEqualTo("supplier@test.com");
		assertThat(subject.getValue()).contains("#42").contains("Cafe Blue Wave");
		assertThat(body.getValue()).contains("9000.00");
	}

	@Test
	@DisplayName("a status change emails the cafe")
	void statusChangeNotifiesCafe() {
		listener.onOrderStatusChanged(new OrderStatusChanged(42L, "cafe@test.com",
				"Cafe Blue Wave", "Ocean Fresh", "ACCEPTED"));

		ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(to.capture(), anyString(), anyString());
		assertThat(to.getValue()).isEqualTo("cafe@test.com");
	}

	@Test
	@DisplayName("a delivery without a driver still produces a sensible message")
	void deliveryWithoutDriverIsReadable() {
		listener.onDeliveryStatusChanged(new DeliveryStatusChanged(42L, "cafe@test.com",
				"SCHEDULED", null, null));

		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), body.capture());

		assertThat(body.getValue())
				.as("must not render a null driver name")
				.doesNotContain("null")
				.contains("not been assigned");
	}

	@Test
	@DisplayName("a driver with no phone number does not render null")
	void driverWithoutPhoneIsReadable() {
		listener.onDeliveryStatusChanged(new DeliveryStatusChanged(42L, "cafe@test.com",
				"IN_TRANSIT", "Nimal", null));

		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), body.capture());

		assertThat(body.getValue()).contains("Nimal").doesNotContain("null");
	}

	@Test
	@DisplayName("a failing mail server never propagates - the order is already committed")
	void mailFailureIsSwallowed() {
		doThrow(new RuntimeException("SMTP unreachable"))
				.when(emailService).sendEmail(anyString(), anyString(), anyString());

		assertThatCode(() -> listener.onOrderPlaced(
				new OrderPlaced(42L, "supplier@test.com", "Ocean Fresh", "Cafe", 100.0)))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("notifications can be switched off entirely")
	void disabledSendsNothing() {
		ReflectionTestUtils.setField(listener, "enabled", false);

		listener.onOrderPlaced(new OrderPlaced(42L, "supplier@test.com", "Ocean Fresh", "Cafe", 100.0));

		verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
	}
}
