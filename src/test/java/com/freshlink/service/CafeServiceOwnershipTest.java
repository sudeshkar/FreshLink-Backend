package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.enums.OrderStatus;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.model.Cafe;
import com.freshlink.model.Order;
import com.freshlink.service.interfaces.impl.CafeServiceImpl;

/**
 * cancelOrder used to look the cafe up by email and then discard the result,
 * so any cafe could cancel any other cafe's order - and cancelling restores
 * stock, corrupting the supplier's inventory too.
 */
@ExtendWith(MockitoExtension.class)
class CafeServiceOwnershipTest {

	@Mock private CafeRepository cafeRepository;
	@Mock private FishRepository fishRepository;
	@Mock private FishMapper fishMapper;
	@Mock private OrderRepository orderRepository;
	@Mock private OrderMapper orderMapper;
	@Mock private DeliveryRepository deliveryRepository;

	@InjectMocks private CafeServiceImpl cafeService;

	private Cafe cafe(Long id, String email) {
		Cafe cafe = new Cafe();
		cafe.setId(id);
		cafe.setEmail(email);
		return cafe;
	}

	@Test
	@DisplayName("a cafe cannot cancel another cafe's order")
	void cancelRejectsForeignOrder() {
		Cafe owner = cafe(1L, "owner@cafe.test");
		Cafe attacker = cafe(2L, "attacker@cafe.test");

		Order order = new Order();
		order.setId(77L);
		order.setCafe(owner);
		order.setStatus(OrderStatus.REQUESTED);

		when(cafeRepository.findByEmail("attacker@cafe.test")).thenReturn(Optional.of(attacker));
		when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> cafeService.cancelOrder(77L, "attacker@cafe.test"))
				.isInstanceOf(ResourceNotFoundException.class);

		// The order must be left completely untouched.
		assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
	}

	@Test
	@DisplayName("the owning cafe can cancel its own pending order")
	void cancelAllowsOwnOrder() {
		Cafe owner = cafe(1L, "owner@cafe.test");

		Order order = new Order();
		order.setId(77L);
		order.setCafe(owner);
		order.setStatus(OrderStatus.REQUESTED);
		order.setItems(java.util.List.of());

		when(cafeRepository.findByEmail("owner@cafe.test")).thenReturn(Optional.of(owner));
		when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

		cafeService.cancelOrder(77L, "owner@cafe.test");

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}
}
