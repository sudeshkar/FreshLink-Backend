package com.freshlink.service.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.deliverydto.DeliveryResponse;
import com.freshlink.deliverydto.DeliveryUpdateRequest;
import com.freshlink.fishdto.FishCreateRequest;
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

	    List<FishResponse> getMyFish(String supplierEmail);

	    FishResponse updateFish(Long id, FishUpdateRequest dto, String supplierEmail);

	    void deleteFish(Long id, String supplierEmail);

		SupplierProfileResponse getProfile(String name);

		OrderResponse acceptOrder(Long orderId, String supplierEmail);

		Page<OrderResponse> getIncomingOrders(String supplierEmail, Pageable pageable);

		void rejectOrder(Long orderId, String supplierEmail);

		void markDelivering(Long orderId, String supplierEmail);

		void completeOrder(Long orderId, String supplierEmail);

		List<SupplyMatchResponse> getPendingMatches(String supplierEmail);

	// ---- Delivery ----

	DeliveryResponse getDelivery(Long orderId, String supplierEmail);

	/** Assigns a driver, sets an ETA, or moves the delivery to its next state. */
	DeliveryResponse updateDelivery(Long orderId, DeliveryUpdateRequest dto, String supplierEmail);

	// ---- Daily supply: the matching engine's input ----

	DailySupplyResponse addDailySupply(DailySupplyCreateRequest dto, String supplierEmail);

	List<DailySupplyResponse> getMyDailySupply(String supplierEmail);

	DailySupplyResponse updateDailySupply(Long id, DailySupplyUpdateRequest dto, String supplierEmail);

	void deleteDailySupply(Long id, String supplierEmail);

		void acceptMatch(Long id, String supplierEmail);

		void rejectMatch(Long id, String supplierEmail);
}
