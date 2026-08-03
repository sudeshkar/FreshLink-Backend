package com.freshlink.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.enums.OrderStatus;
import com.freshlink.deliverydto.DeliveryResponse;
import com.freshlink.fishdto.PriceHistoryResponse;
import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.userprofiledto.CafeProfileResponse;

public interface CafeService {

	CafeProfileResponse getProfile(String name);

	Page<FishMarketResponse> browseFish(String fishType, String city, Pageable pageable);

	OrderResponse placeOrder(OrderCreateRequest dto, String email);

	/** {@code status} is optional; null returns every order. */
	Page<OrderResponse> getOrders(String email, OrderStatus status, Pageable pageable);

	void cancelOrder(Long orderId, String cafeEmail);

	/** Price trend for a listing on the market, so a cafe can judge today's offer. */
	Page<PriceHistoryResponse> getPriceHistory(Long fishId, Pageable pageable);

	/** Where the cafe's order currently is. */
	DeliveryResponse trackDelivery(Long orderId, String cafeEmail);

}
