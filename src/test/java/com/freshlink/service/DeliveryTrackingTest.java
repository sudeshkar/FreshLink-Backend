package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.deliverydto.DeliveryUpdateRequest;
import com.freshlink.enums.DeliveryStatus;
import com.freshlink.enums.OrderStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.Cafe;
import com.freshlink.model.Delivery;
import com.freshlink.model.Order;
import com.freshlink.model.Supplier;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;

@ExtendWith(MockitoExtension.class)
class DeliveryTrackingTest {

	@Mock private FishRepository fishRepository;
	@Mock private SupplierRepository supplierRepository;
	@Mock private FishMapper fishMapper;
	@Mock private FishTypeRepository fishTypeRepository;
	@Mock private SupplierMapper supplierMapper;
	@Mock private OrderRepository orderRepository;
	@Mock private OrderMapper orderMapper;
	@Mock private SupplyMatchRepository supplyMatchRepository;
	@Mock private DailySupplyRepository dailySupplyRepository;
	@Mock private DemandRepository demandRepository;
	@Mock private DeliveryRepository deliveryRepository;
	@Mock private DemandMatchingScheduler demandMatchingScheduler;
	@Mock private DemandMatchService demandMatchService;

	@InjectMocks private SupplierServiceImpl supplierService;

	private static final String SUPPLIER_EMAIL = "owner@supplier.test";

	private Supplier supplier() {
		Supplier supplier = new Supplier();
		supplier.setId(1L);
		supplier.setEmail(SUPPLIER_EMAIL);
		return supplier;
	}

	private Order order(Supplier owner, OrderStatus status) {
		Cafe cafe = new Cafe();
		cafe.setId(9L);

		Order order = new Order();
		order.setId(100L);
		order.setSupplier(owner);
		order.setCafe(cafe);
		order.setStatus(status);
		return order;
	}

	private Delivery delivery(Order order, DeliveryStatus status) {
		Delivery delivery = new Delivery();
		delivery.setDeliveryId(5L);
		delivery.setOrder(order);
		delivery.setStatus(status);
		delivery.setDeliveryDate(LocalDateTime.now());
		return delivery;
	}

	private void stubOwnOrder(Order order) {
		when(supplierRepository.findByEmail(SUPPLIER_EMAIL)).thenReturn(Optional.of(order.getSupplier()));
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
	}

	@Test
	@DisplayName("SCHEDULED can move to IN_TRANSIT")
	void scheduledToInTransit() {
		Order order = order(supplier(), OrderStatus.DELIVERING);
		Delivery delivery = delivery(order, DeliveryStatus.SCHEDULED);

		stubOwnOrder(order);
		when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));
		when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateDelivery(100L,
				new DeliveryUpdateRequest(DeliveryStatus.IN_TRANSIT, "Nimal", "0771234567", null, null),
				SUPPLIER_EMAIL);

		assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
		assertThat(delivery.getDriverName()).isEqualTo("Nimal");
		assertThat(delivery.getDeliveredAt())
				.as("nothing has arrived yet")
				.isNull();
	}

	@Test
	@DisplayName("arrival time is stamped by the server, not accepted from the caller")
	void deliveredStampsArrivalTime() {
		Order order = order(supplier(), OrderStatus.DELIVERING);
		Delivery delivery = delivery(order, DeliveryStatus.IN_TRANSIT);

		stubOwnOrder(order);
		when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));
		when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		LocalDateTime before = LocalDateTime.now().minusSeconds(1);

		supplierService.updateDelivery(100L,
				new DeliveryUpdateRequest(DeliveryStatus.DELIVERED, null, null, null, "Left with manager"),
				SUPPLIER_EMAIL);

		assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
		assertThat(delivery.getDeliveredAt()).isAfter(before);
		assertThat(delivery.getNotes()).isEqualTo("Left with manager");
	}

	@Test
	@DisplayName("a delivered delivery is final")
	void deliveredIsTerminal() {
		Order order = order(supplier(), OrderStatus.COMPLETED);
		Delivery delivery = delivery(order, DeliveryStatus.DELIVERED);

		stubOwnOrder(order);
		when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));

		assertThatThrownBy(() -> supplierService.updateDelivery(100L,
				new DeliveryUpdateRequest(DeliveryStatus.IN_TRANSIT, null, null, null, null),
				SUPPLIER_EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot move from DELIVERED");

		verify(deliveryRepository, never()).save(any());
	}

	@Test
	@DisplayName("SCHEDULED cannot jump straight to DELIVERED")
	void cannotSkipInTransit() {
		Order order = order(supplier(), OrderStatus.DELIVERING);
		Delivery delivery = delivery(order, DeliveryStatus.SCHEDULED);

		stubOwnOrder(order);
		when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));

		assertThatThrownBy(() -> supplierService.updateDelivery(100L,
				new DeliveryUpdateRequest(DeliveryStatus.DELIVERED, null, null, null, null),
				SUPPLIER_EMAIL))
				.isInstanceOf(BusinessRuleException.class);

		assertThat(delivery.getDeliveredAt()).isNull();
	}

	@Test
	@DisplayName("a failed attempt can be retried")
	void failedCanReturnToInTransit() {
		Order order = order(supplier(), OrderStatus.DELIVERING);
		Delivery delivery = delivery(order, DeliveryStatus.FAILED);

		stubOwnOrder(order);
		when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));
		when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateDelivery(100L,
				new DeliveryUpdateRequest(DeliveryStatus.IN_TRANSIT, null, null, null, "Second attempt"),
				SUPPLIER_EMAIL);

		assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
	}

	@Test
	@DisplayName("a supplier cannot touch another supplier's delivery")
	void cannotUpdateForeignDelivery() {
		Supplier owner = supplier();
		Supplier attacker = new Supplier();
		attacker.setId(2L);
		attacker.setEmail("attacker@supplier.test");

		Order order = order(owner, OrderStatus.DELIVERING);

		when(supplierRepository.findByEmail("attacker@supplier.test")).thenReturn(Optional.of(attacker));
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> supplierService.updateDelivery(100L,
				new DeliveryUpdateRequest(DeliveryStatus.IN_TRANSIT, null, null, null, null),
				"attacker@supplier.test"))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(deliveryRepository, never()).save(any());
	}

	@Test
	@DisplayName("completing the order settles the delivery, so the two cannot drift apart")
	void completingOrderMarksDeliveryDelivered() {
		Order order = order(supplier(), OrderStatus.DELIVERING);
		Delivery delivery = delivery(order, DeliveryStatus.IN_TRANSIT);

		stubOwnOrder(order);
		when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));

		supplierService.completeOrder(100L, SUPPLIER_EMAIL);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
		assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
		assertThat(delivery.getDeliveredAt()).isNotNull();
	}
}
