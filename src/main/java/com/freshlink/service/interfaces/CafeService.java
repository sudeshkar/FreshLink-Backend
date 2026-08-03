package com.freshlink.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.deliverydto.DeliveryResponse;
import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.userprofiledto.CafeProfileResponse;

public interface CafeService {

	CafeProfileResponse getProfile(String name);

	Page<FishMarketResponse> browseFish(String fishType, String city, Pageable pageable);

	OrderResponse placeOrder(OrderCreateRequest dto, String email);

	Page<OrderResponse> getOrders(String email, Pageable pageable);

	void cancelOrder(Long orderId, String cafeEmail);

	/** Where the cafe's order currently is. */
	DeliveryResponse trackDelivery(Long orderId, String cafeEmail);

}
