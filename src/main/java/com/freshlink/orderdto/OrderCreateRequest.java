package com.freshlink.orderdto;

import java.util.List;

public record OrderCreateRequest(
		  List<OrderItemDto> items
		) {

}
