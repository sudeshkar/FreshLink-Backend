package com.freshlink.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.model.Fish;
import com.freshlink.model.Supplier;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderItemDto;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.service.interfaces.CafeService;
import com.freshlink.service.interfaces.SupplierService;

/**
 * The revenue figures are JPQL, so only a real query against a real database
 * proves what they count. They previously summed every order item regardless of
 * status, meaning rejected and cancelled orders were reported to the admin as
 * money earned - while the supplier leaderboard on the same dashboard filtered
 * on COMPLETED, so the two disagreed.
 */
@SpringBootTest(properties = {
		"app.ratelimit.enabled=false",
		"app.notifications.enabled=false"
})
@ActiveProfiles("dev")
class RevenueAccountingIT {

	@Autowired private CafeService cafeService;
	@Autowired private SupplierService supplierService;
	@Autowired private OrderRepository orderRepository;
	@Autowired private FishRepository fishRepository;
	@Autowired private SupplierRepository supplierRepository;

	private static final String CAFE = "cafe1@freshlink.com";
	private static final String SUPPLIER = "supplier1@freshlink.com";

	/**
	 * Queries by supplier rather than filtering findAll on {@code getSupplier()}:
	 * the association is lazy and there is no session open out here.
	 */
	private Fish anyListingOf(String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail).orElseThrow();
		return fishRepository.findBySupplier(supplier).stream()
				.filter(f -> f.getAvailableKg() > 10)
				.findFirst()
				.orElseThrow();
	}

	private OrderResponse place(Fish fish, double qty) {
		return cafeService.placeOrder(
				new OrderCreateRequest(java.util.List.of(new OrderItemDto(fish.getId(), qty))),
				CAFE);
	}

	@Test
	@DisplayName("a cancelled order contributes nothing to revenue")
	void cancelledOrderIsNotRevenue() {
		BigDecimal before = orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO);

		Fish fish = anyListingOf(SUPPLIER);
		OrderResponse order = place(fish, 3.0);
		cafeService.cancelOrder(order.orderId(), CAFE);

		BigDecimal after = orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO);

		assertThat(after)
				.as("a cancelled order was never earned")
				.isEqualByComparingTo(before);
	}

	@Test
	@DisplayName("a rejected order contributes nothing to revenue")
	void rejectedOrderIsNotRevenue() {
		BigDecimal before = orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO);

		Fish fish = anyListingOf(SUPPLIER);
		OrderResponse order = place(fish, 3.0);
		supplierService.rejectOrder(order.orderId(), SUPPLIER);

		BigDecimal after = orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO);

		assertThat(after).isEqualByComparingTo(before);
	}

	@Test
	@DisplayName("an order in flight is not counted until it completes")
	void revenueIsRecognisedOnlyOnCompletion() {
		Fish fish = anyListingOf(SUPPLIER);
		BigDecimal before = orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO);

		OrderResponse order = place(fish, 2.0);
		BigDecimal expected = fish.getPricePerKg().multiply(BigDecimal.valueOf(2.0));

		supplierService.acceptOrder(order.orderId(), SUPPLIER);
		assertThat(orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO))
				.as("accepted is not yet delivered")
				.isEqualByComparingTo(before);

		supplierService.markDelivering(order.orderId(), SUPPLIER);
		assertThat(orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO))
				.as("on the road is still not delivered")
				.isEqualByComparingTo(before);

		supplierService.completeOrder(order.orderId(), SUPPLIER);
		assertThat(orderRepository.sumTotalOrderValue().orElse(BigDecimal.ZERO))
				.as("now it counts")
				.isEqualByComparingTo(before.add(expected));
	}
}
