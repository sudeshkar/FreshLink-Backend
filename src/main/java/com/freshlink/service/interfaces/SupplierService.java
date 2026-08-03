package com.freshlink.service.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.deliverydto.DeliveryResponse;
import com.freshlink.deliverydto.DeliveryUpdateRequest;
import com.freshlink.enums.OrderStatus;
import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.PriceHistoryResponse;
import com.freshlink.routedto.RouteCreateRequest;
import com.freshlink.routedto.RouteResponse;
import com.freshlink.routedto.RouteStatusUpdateRequest;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.supplydto.DailySupplyCreateRequest;
import com.freshlink.supplydto.DailySupplyResponse;
import com.freshlink.supplydto.DailySupplyUpdateRequest;
import com.freshlink.supplymatch.dto.SupplyMatchResponse;
import com.freshlink.userprofiledto.SupplierProfileResponse;

public interface SupplierService {
		FishResponse addFish(FishCreateRequest dto, String supplierEmail);

	    Page<FishResponse> getMyFish(String supplierEmail, Pageable pageable);

	    FishResponse updateFish(Long id, FishUpdateRequest dto, String supplierEmail);

	    void deleteFish(Long id, String supplierEmail);

		SupplierProfileResponse getProfile(String name);

		OrderResponse acceptOrder(Long orderId, String supplierEmail);

		/** {@code status} is optional; null returns every order. */
		Page<OrderResponse> getIncomingOrders(String supplierEmail, OrderStatus status, Pageable pageable);

		void rejectOrder(Long orderId, String supplierEmail);

		void markDelivering(Long orderId, String supplierEmail);

		void completeOrder(Long orderId, String supplierEmail);

		List<SupplyMatchResponse> getPendingMatches(String supplierEmail);

	/** What this listing has charged over time, newest first. */
	Page<PriceHistoryResponse> getFishPriceHistory(Long fishId, String supplierEmail, Pageable pageable);

	// ---- Delivery routes: one driver, one trip, several drop-offs ----

	RouteResponse createRoute(RouteCreateRequest dto, String supplierEmail);

	Page<RouteResponse> getRoutes(String supplierEmail, Pageable pageable);

	RouteResponse getRoute(Long routeId, String supplierEmail);

	/** Dispatching moves every stop onto the road and stamps the driver on each. */
	RouteResponse updateRouteStatus(Long routeId, RouteStatusUpdateRequest dto, String supplierEmail);

	/** Adds one drop-off to a route that is still being planned. */
	RouteResponse addStop(Long routeId, Long orderId, String supplierEmail);

	/** Takes a drop-off off a route. The delivery survives, unassigned. */
	RouteResponse removeStop(Long routeId, Long orderId, String supplierEmail);

	/** Only while still planned. Deletes the route, never its deliveries. */
	void deleteRoute(Long routeId, String supplierEmail);

	// ---- Delivery ----

	DeliveryResponse getDelivery(Long orderId, String supplierEmail);

	/** Assigns a driver, sets an ETA, or moves the delivery to its next state. */
	DeliveryResponse updateDelivery(Long orderId, DeliveryUpdateRequest dto, String supplierEmail);

	// ---- Daily supply: the matching engine's input ----

	DailySupplyResponse addDailySupply(DailySupplyCreateRequest dto, String supplierEmail);

	Page<DailySupplyResponse> getMyDailySupply(String supplierEmail, Pageable pageable);

	DailySupplyResponse updateDailySupply(Long id, DailySupplyUpdateRequest dto, String supplierEmail);

	void deleteDailySupply(Long id, String supplierEmail);

		void acceptMatch(Long id, String supplierEmail);

		void rejectMatch(Long id, String supplierEmail);
}
