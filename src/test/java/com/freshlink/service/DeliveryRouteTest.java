package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.DeliveryRouteRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishPriceHistoryRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.enums.DeliveryStatus;
import com.freshlink.enums.OrderStatus;
import com.freshlink.enums.RouteStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.Cafe;
import com.freshlink.model.Delivery;
import com.freshlink.model.DeliveryRoute;
import com.freshlink.model.Order;
import com.freshlink.model.Supplier;
import com.freshlink.routedto.RouteCreateRequest;
import com.freshlink.routedto.RouteStatusUpdateRequest;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.impl.SupplierServiceImpl;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteTest {

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
	@Mock private FishPriceHistoryRepository fishPriceHistoryRepository;
	@Mock private ApplicationEventPublisher events;
	@Mock private DeliveryRouteRepository deliveryRouteRepository;

	@InjectMocks private SupplierServiceImpl supplierService;

	private static final String EMAIL = "owner@supplier.test";

	private Supplier supplier() {
		Supplier supplier = new Supplier();
		supplier.setId(1L);
		supplier.setEmail(EMAIL);
		return supplier;
	}

	private Order order(Long id, Supplier owner) {
		Cafe cafe = new Cafe();
		cafe.setId(9L);
		cafe.setName("Cafe Blue Wave");
		cafe.setEmail("cafe@test.com");
		cafe.setAddress("1 Main St");

		Order order = new Order();
		order.setId(id);
		order.setSupplier(owner);
		order.setCafe(cafe);
		order.setStatus(OrderStatus.DELIVERING);
		return order;
	}

	private Delivery delivery(Order order, DeliveryStatus status) {
		Delivery delivery = new Delivery();
		delivery.setDeliveryId(order.getId() + 100);
		delivery.setOrder(order);
		delivery.setStatus(status);
		return delivery;
	}

	private DeliveryRoute route(Supplier owner, RouteStatus status) {
		DeliveryRoute route = new DeliveryRoute();
		route.setId(7L);
		route.setSupplier(owner);
		route.setStatus(status);
		route.setRouteDate(LocalDate.now());
		route.setDriverName("Nimal");
		route.setDriverPhone("0771234567");
		return route;
	}

	@Test
	@DisplayName("a route groups several orders into one trip")
	void createGroupsOrders() {
		Supplier owner = supplier();
		Order first = order(1L, owner);
		Order second = order(2L, owner);
		Delivery d1 = delivery(first, DeliveryStatus.SCHEDULED);
		Delivery d2 = delivery(second, DeliveryStatus.SCHEDULED);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.save(any())).thenAnswer(inv -> {
			DeliveryRoute r = inv.getArgument(0);
			r.setId(7L);
			return r;
		});
		when(orderRepository.findById(1L)).thenReturn(Optional.of(first));
		when(orderRepository.findById(2L)).thenReturn(Optional.of(second));
		when(deliveryRepository.findByOrder(first)).thenReturn(Optional.of(d1));
		when(deliveryRepository.findByOrder(second)).thenReturn(Optional.of(d2));
		when(deliveryRepository.findByRoute(any())).thenReturn(List.of(d1, d2));

		var response = supplierService.createRoute(
				new RouteCreateRequest(LocalDate.now(), "Nimal", "0771234567", null, List.of(1L, 2L)),
				EMAIL);

		assertThat(response.stopCount()).isEqualTo(2);
		assertThat(d1.getRoute()).isNotNull();
		assertThat(d2.getRoute()).isNotNull();
	}

	@Test
	@DisplayName("an order that is not out for delivery cannot be routed")
	void cannotRouteAnUndeliveredOrder() {
		Supplier owner = supplier();
		Order notDelivering = order(1L, owner);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.findById(1L)).thenReturn(Optional.of(notDelivering));
		when(deliveryRepository.findByOrder(notDelivering)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> supplierService.createRoute(
				new RouteCreateRequest(LocalDate.now(), "Nimal", null, null, List.of(1L)), EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not out for delivery");
	}

	@Test
	@DisplayName("another supplier's order cannot be added to a route")
	void cannotRouteForeignOrder() {
		Supplier owner = supplier();
		Supplier other = new Supplier();
		other.setId(2L);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, other)));

		assertThatThrownBy(() -> supplierService.createRoute(
				new RouteCreateRequest(LocalDate.now(), "Nimal", null, null, List.of(1L)), EMAIL))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("dispatching puts every stop on the road and stamps the driver on each")
	void dispatchAdvancesEveryStop() {
		Supplier owner = supplier();
		DeliveryRoute route = route(owner, RouteStatus.PLANNED);
		Delivery d1 = delivery(order(1L, owner), DeliveryStatus.SCHEDULED);
		Delivery d2 = delivery(order(2L, owner), DeliveryStatus.SCHEDULED);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.findByIdAndSupplier(7L, owner)).thenReturn(Optional.of(route));
		when(deliveryRepository.findByRoute(route)).thenReturn(List.of(d1, d2));
		when(deliveryRouteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateRouteStatus(7L, new RouteStatusUpdateRequest(RouteStatus.DISPATCHED), EMAIL);

		assertThat(route.getStatus()).isEqualTo(RouteStatus.DISPATCHED);
		assertThat(d1.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
		assertThat(d2.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
		assertThat(d1.getDriverName()).isEqualTo("Nimal");
		assertThat(d2.getDriverPhone()).isEqualTo("0771234567");
	}

	@Test
	@DisplayName("a route cannot complete while a stop is still outstanding")
	void cannotCompleteWithOutstandingStops() {
		Supplier owner = supplier();
		DeliveryRoute route = route(owner, RouteStatus.DISPATCHED);
		Delivery done = delivery(order(1L, owner), DeliveryStatus.DELIVERED);
		Delivery stillOut = delivery(order(2L, owner), DeliveryStatus.IN_TRANSIT);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.findByIdAndSupplier(7L, owner)).thenReturn(Optional.of(route));
		when(deliveryRepository.findByRoute(route)).thenReturn(List.of(done, stillOut));

		assertThatThrownBy(() -> supplierService.updateRouteStatus(
				7L, new RouteStatusUpdateRequest(RouteStatus.COMPLETED), EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("1 stop(s)");

		assertThat(route.getStatus()).isEqualTo(RouteStatus.DISPATCHED);
	}

	@Test
	@DisplayName("a failed stop still counts as resolved, so one bad drop cannot block the route")
	void failedStopsCountAsResolved() {
		Supplier owner = supplier();
		DeliveryRoute route = route(owner, RouteStatus.DISPATCHED);
		Delivery done = delivery(order(1L, owner), DeliveryStatus.DELIVERED);
		Delivery failed = delivery(order(2L, owner), DeliveryStatus.FAILED);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.findByIdAndSupplier(7L, owner)).thenReturn(Optional.of(route));
		when(deliveryRepository.findByRoute(route)).thenReturn(List.of(done, failed));
		when(deliveryRouteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateRouteStatus(7L, new RouteStatusUpdateRequest(RouteStatus.COMPLETED), EMAIL);

		assertThat(route.getStatus()).isEqualTo(RouteStatus.COMPLETED);
	}

	@Test
	@DisplayName("cancelling detaches the stops rather than losing them")
	void cancelDetachesStops() {
		Supplier owner = supplier();
		DeliveryRoute route = route(owner, RouteStatus.PLANNED);
		Delivery d1 = delivery(order(1L, owner), DeliveryStatus.SCHEDULED);
		d1.setRoute(route);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.findByIdAndSupplier(7L, owner)).thenReturn(Optional.of(route));
		when(deliveryRepository.findByRoute(route)).thenReturn(List.of(d1));
		when(deliveryRouteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		supplierService.updateRouteStatus(7L, new RouteStatusUpdateRequest(RouteStatus.CANCELLED), EMAIL);

		assertThat(d1.getRoute()).as("the delivery still has to happen, just on another van").isNull();
		verify(deliveryRepository).save(d1);
		verify(deliveryRepository, never()).delete(any());
	}

	@Test
	@DisplayName("a dispatched route cannot be deleted")
	void cannotDeleteDispatchedRoute() {
		Supplier owner = supplier();
		DeliveryRoute route = route(owner, RouteStatus.DISPATCHED);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.findByIdAndSupplier(7L, owner)).thenReturn(Optional.of(route));

		assertThatThrownBy(() -> supplierService.deleteRoute(7L, EMAIL))
				.isInstanceOf(BusinessRuleException.class);

		verify(deliveryRouteRepository, never()).delete(any());
	}

	@Test
	@DisplayName("a completed route is terminal")
	void completedRouteIsTerminal() {
		Supplier owner = supplier();
		DeliveryRoute route = route(owner, RouteStatus.COMPLETED);

		when(supplierRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
		when(deliveryRouteRepository.findByIdAndSupplier(7L, owner)).thenReturn(Optional.of(route));

		assertThatThrownBy(() -> supplierService.updateRouteStatus(
				7L, new RouteStatusUpdateRequest(RouteStatus.DISPATCHED), EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot move from COMPLETED");
	}
}
