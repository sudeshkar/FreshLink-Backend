package com.freshlink.service.interfaces.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.DeliveryRouteRepository;
import com.freshlink.Repository.DeliveryRepository;
import com.freshlink.Repository.DemandRepository;
import com.freshlink.Repository.FishPriceHistoryRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.SupplyMatchRepository;
import com.freshlink.deliverydto.DeliveryResponse;
import com.freshlink.deliverydto.DeliveryUpdateRequest;
import com.freshlink.enums.RouteStatus;
import com.freshlink.enums.DeliveryStatus;
import com.freshlink.enums.DemandStatus;
import com.freshlink.enums.MatchStatus;
import com.freshlink.enums.OrderStatus;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.PriceHistoryResponse;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.mapper.SupplyMatchMapper;
import com.freshlink.model.DailySupply;
import com.freshlink.model.DeliveryRoute;
import com.freshlink.model.Delivery;
import com.freshlink.model.DemandRequest;
import com.freshlink.model.FishPriceHistory;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Order;
import com.freshlink.model.OrderItem;
import com.freshlink.model.Supplier;
import com.freshlink.model.SupplyMatch;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.notification.NotificationEvents;
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.SupplierService;
import com.freshlink.routedto.RouteCreateRequest;
import com.freshlink.routedto.RouteResponse;
import com.freshlink.routedto.RouteStatusUpdateRequest;
import com.freshlink.routedto.RouteStopResponse;
import com.freshlink.supplydto.DailySupplyCreateRequest;
import com.freshlink.supplydto.DailySupplyResponse;
import com.freshlink.supplydto.DailySupplyUpdateRequest;
import com.freshlink.supplymatch.dto.SupplyMatchResponse;
import com.freshlink.userprofiledto.SupplierProfileResponse;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService{
	private final FishRepository fishRepository;
    private final SupplierRepository supplierRepository;
    private final FishMapper fishMapper;
    private final FishTypeRepository fishTypeRepository;
    private final SupplierMapper supplierMapper;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final SupplyMatchRepository supplyMatchRepository;
    private final DailySupplyRepository dailySupplyRepository;
    private final DemandRepository demandRepository;
    private final DeliveryRepository deliveryRepository;
    private final DemandMatchingScheduler demandMatchingScheduler;
    private final DemandMatchService demandMatchService;
    private final FishPriceHistoryRepository fishPriceHistoryRepository;
    private final ApplicationEventPublisher events;
    private final DeliveryRouteRepository deliveryRouteRepository;
    
	@Override
	
	public FishResponse addFish(FishCreateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));
		FishType fishType = fishTypeRepository.findByNameIgnoreCase(dto.fishTypeName())
	            .orElseThrow(() -> new ResourceNotFoundException("Fish type", dto.fishTypeName()));
		
		if (fishRepository.existsBySupplierAndFishType(supplier, fishType)) {
		    throw new BusinessRuleException("You already have a listing for this fish type. Update it instead.");
		}

		
        Fish fish = new Fish();
        fish.setName(dto.name());
        fish.setPricePerKg(dto.pricePerKg());
        fish.setAvailableKg(dto.availableKg());
        fish.setFishType(fishType);    
        fish.setSupplier(supplier);

        Fish saved = fishRepository.save(fish);
        recordPrice(saved);
        return fishMapper.toFishResponse(saved);
	}
	@Override
	public List<FishResponse> getMyFish(String supplierEmail) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));
	        return fishRepository.findBySupplier(supplier)
	                .stream()
	                .map(fishMapper::toFishResponse) 
	                .collect(Collectors.toList());
	}
	@Override
	public FishResponse updateFish(Long id, FishUpdateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));
		
		Fish fish = fishRepository.findById(id)
				.filter(f -> f.getSupplier().getId().equals(supplier.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Fish", id));

		 if (dto.name() != null && !dto.name().isBlank()) {
		        fish.setName(dto.name());
		    }
		 // Compared before assignment: only an actual change is worth a history
		 // row, otherwise every unrelated edit adds noise to the price trend.
		 boolean priceChanged = dto.pricePerKg() != null
				 && dto.pricePerKg().compareTo(fish.getPricePerKg()) != 0;
		 if (dto.pricePerKg() != null) fish.setPricePerKg(dto.pricePerKg());
        if (dto.availableKg() != null) fish.setAvailableKg(dto.availableKg());
        if (dto.fishTypeName() != null && !dto.fishTypeName().isBlank()) {
            FishType fishType = fishTypeRepository
                    .findByNameIgnoreCase(dto.fishTypeName())
                    .orElseThrow(() -> new ResourceNotFoundException("Fish type", dto.fishTypeName()));

            fish.setFishType(fishType);
        }

        
        Fish updated = fishRepository.save(fish);
        if (priceChanged) {
            recordPrice(updated);
        }
        return fishMapper.toFishResponse(updated);
}
	@Override
	public void deleteFish(Long id, String supplierEmail) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

	        Fish fish = fishRepository.findById(id)
	                .filter(f -> f.getSupplier().getId().equals(supplier.getId()))
	                .orElseThrow(() -> new ResourceNotFoundException("Fish", id));

	        fishRepository.delete(fish);
		
	}
	
	@Override
	public SupplierProfileResponse getProfile(String supplierEmail) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				 .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));
		 
		 return supplierMapper.toProfile(supplier);
	}
	
	@Override
	public OrderResponse acceptOrder(Long orderId, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		
		if (order.getStatus() != OrderStatus.REQUESTED) {
	        throw new BusinessRuleException("Only an order awaiting your response can be accepted");
	    }
		
		for (OrderItem item : order.getItems()) {
	        Fish fish = item.getFish();

	        fish.setReservedKg(
	            fish.getReservedKg() - item.getQuantityKg()
	        );
	         
	    }
		 order.setStatus(OrderStatus.ACCEPTED);
		 publishOrderStatus(order);
		 
		 
		  
		 
		 return orderMapper.toOrderResponse(order);
 
		
	}
	@Override
	public Page<OrderResponse> getIncomingOrders(String supplierEmail, Pageable pageable) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
		            .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));
		 return orderRepository.findBySupplier(supplier, pageable)
				 .map(orderMapper::toOrderResponse);
	}
	@Override
	public void rejectOrder(Long orderId, String supplierEmail) {
		 Order order = validateSupplierOrder(orderId, supplierEmail);
		 
		 if (order.getStatus() != OrderStatus.REQUESTED) {
		        throw new BusinessRuleException("Only an order awaiting your response can be rejected");
		    }
		 
		 for (OrderItem item : order.getItems()) {
		        Fish fish = item.getFish();

		        fish.setAvailableKg(
		            fish.getAvailableKg() + item.getQuantityKg()
		        );
		        fish.setReservedKg(
		            fish.getReservedKg() - item.getQuantityKg()
		        );
		    }
		 order.setStatus(OrderStatus.REJECTED);
		 publishOrderStatus(order);

		  
		
	}
	@Override
	public void markDelivering(Long orderId, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		if (order.getStatus() != OrderStatus.ACCEPTED) {
	        throw new BusinessRuleException("Only an accepted order can be marked as out for delivery");
	    }
		
		order.setStatus(OrderStatus.DELIVERING);
		Delivery delivery = new Delivery();
		delivery.setDeliveryDate(LocalDateTime.now());
		delivery.setOrder(order);
		delivery.setStatus(DeliveryStatus.SCHEDULED);
		
		deliveryRepository.save(delivery);
		
	}
	
	@Override
	public void completeOrder(Long orderId, String supplierEmail) {
		
		Order order = validateSupplierOrder(orderId, supplierEmail);

	    if (order.getStatus() != OrderStatus.DELIVERING) {
	        throw new BusinessRuleException("Only an order that is out for delivery can be completed");
	    }

	    order.setStatus(OrderStatus.COMPLETED);
	    publishOrderStatus(order);

	    // Completing the order settles the delivery too, so the two cannot drift
	    // apart and leave a delivered order tracking as still scheduled.
	    deliveryRepository.findByOrder(order).ifPresent(delivery -> {
	        if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
	            delivery.setStatus(DeliveryStatus.DELIVERED);
	            delivery.setDeliveredAt(LocalDateTime.now());
	            deliveryRepository.save(delivery);
	        }
	    });
	}
	
	private Order validateSupplierOrder(Long orderId, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	            .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

	    return orderRepository.findById(orderId)
	            .filter(order -> order.getSupplier().getId().equals(supplier.getId()))
	            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
	}
	@Override
	public List<SupplyMatchResponse> getPendingMatches(String supplierEmail) {
		
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	            .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));
		
		 List<SupplyMatch> supplyMatches = supplyMatchRepository.findByDailySupply_SupplierAndStatus(supplier,MatchStatus.PENDING);
		 List<SupplyMatchResponse> responses = new ArrayList<SupplyMatchResponse>();
		 
		 for (SupplyMatch supplyMatch : supplyMatches) {
			 SupplyMatchResponse response =SupplyMatchMapper.toResponse(supplyMatch);
			 responses.add(response);
		}
		 
		 return responses;
		 
	}
	

	// ---------------- DELIVERY ROUTES ----------------

	/** Legal route moves; COMPLETED and CANCELLED are terminal. */
	private static final Map<RouteStatus, Set<RouteStatus>> ALLOWED_ROUTE_MOVES = Map.of(
			RouteStatus.PLANNED, Set.of(RouteStatus.DISPATCHED, RouteStatus.CANCELLED),
			RouteStatus.DISPATCHED, Set.of(RouteStatus.COMPLETED, RouteStatus.CANCELLED),
			RouteStatus.COMPLETED, Set.of(),
			RouteStatus.CANCELLED, Set.of());

	@Override
	public RouteResponse createRoute(RouteCreateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		DeliveryRoute route = new DeliveryRoute();
		route.setSupplier(supplier);
		route.setRouteDate(dto.routeDate());
		route.setDriverName(dto.driverName());
		route.setDriverPhone(dto.driverPhone());
		route.setNotes(dto.notes());
		route.setStatus(RouteStatus.PLANNED);
		DeliveryRoute saved = deliveryRouteRepository.save(route);

		for (Long orderId : dto.orderIds().stream().distinct().toList()) {
			Order order = orderRepository.findById(orderId)
					.filter(o -> o.getSupplier().getId().equals(supplier.getId()))
					.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

			Delivery delivery = deliveryRepository.findByOrder(order)
					.orElseThrow(() -> new BusinessRuleException(
							"Order %d is not out for delivery yet, so it cannot be routed."
									.formatted(orderId)));

			// A stop belongs to one van. Silently moving it would leave the other
			// route a drop-off short with nothing to show why.
			if (delivery.getRoute() != null && !delivery.getRoute().getId().equals(saved.getId())) {
				throw new BusinessRuleException(
						"Order %d is already on route %d. Remove it from that route first."
								.formatted(orderId, delivery.getRoute().getId()));
			}
			if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
				throw new BusinessRuleException(
						"Order %d has already been delivered.".formatted(orderId));
			}

			delivery.setRoute(saved);
			deliveryRepository.save(delivery);
		}

		return toRouteResponse(saved);
	}

	@Override
	public Page<RouteResponse> getRoutes(String supplierEmail, Pageable pageable) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		return deliveryRouteRepository.findBySupplierOrderByRouteDateDesc(supplier, pageable)
				.map(this::toRouteResponse);
	}

	@Override
	public RouteResponse getRoute(Long routeId, String supplierEmail) {
		return toRouteResponse(requireOwnRoute(routeId, supplierEmail));
	}

	@Override
	public RouteResponse updateRouteStatus(Long routeId, RouteStatusUpdateRequest dto, String supplierEmail) {
		DeliveryRoute route = requireOwnRoute(routeId, supplierEmail);
		RouteStatus target = dto.status();

		if (target != route.getStatus()
				&& !ALLOWED_ROUTE_MOVES.getOrDefault(route.getStatus(), Set.of()).contains(target)) {
			throw new BusinessRuleException(
					"A route cannot move from %s to %s".formatted(route.getStatus(), target));
		}

		List<Delivery> stops = deliveryRepository.findByRoute(route);

		switch (target) {
			case DISPATCHED -> dispatch(route, stops);
			case COMPLETED -> requireEveryStopResolved(stops);
			case CANCELLED -> stops.forEach(stop -> {
				// Detach rather than delete: the deliveries still have to happen,
				// they just need putting on a different van.
				stop.setRoute(null);
				deliveryRepository.save(stop);
			});
			default -> { }
		}

		route.setStatus(target);
		return toRouteResponse(deliveryRouteRepository.save(route));
	}

	@Override
	public void deleteRoute(Long routeId, String supplierEmail) {
		DeliveryRoute route = requireOwnRoute(routeId, supplierEmail);

		if (route.getStatus() != RouteStatus.PLANNED) {
			throw new BusinessRuleException(
					"Only a route that has not been dispatched can be deleted. Cancel it instead.");
		}

		// Unassign the stops first - deleting a route must never delete deliveries.
		deliveryRepository.findByRoute(route).forEach(stop -> {
			stop.setRoute(null);
			deliveryRepository.save(stop);
		});

		deliveryRouteRepository.delete(route);
	}

	/**
	 * Dispatching is the point of a route: the driver is stamped onto every stop
	 * at once and they all go on the road together, instead of being retyped and
	 * advanced one delivery at a time.
	 */
	private void dispatch(DeliveryRoute route, List<Delivery> stops) {
		if (stops.isEmpty()) {
			throw new BusinessRuleException("A route with no stops cannot be dispatched");
		}

		for (Delivery stop : stops) {
			if (stop.getStatus() == DeliveryStatus.SCHEDULED || stop.getStatus() == DeliveryStatus.FAILED) {
				stop.setStatus(DeliveryStatus.IN_TRANSIT);
			}
			if (route.getDriverName() != null) {
				stop.setDriverName(route.getDriverName());
			}
			if (route.getDriverPhone() != null) {
				stop.setDriverPhone(route.getDriverPhone());
			}
			deliveryRepository.save(stop);

			events.publishEvent(new NotificationEvents.DeliveryStatusChanged(
					stop.getOrder().getId(),
					stop.getOrder().getCafe().getEmail(),
					stop.getStatus().name(),
					stop.getDriverName(),
					stop.getDriverPhone()));
		}
	}

	private void requireEveryStopResolved(List<Delivery> stops) {
		long outstanding = stops.stream()
				.filter(s -> s.getStatus() != DeliveryStatus.DELIVERED
						&& s.getStatus() != DeliveryStatus.FAILED)
				.count();

		if (outstanding > 0) {
			throw new BusinessRuleException(
					"%d stop(s) on this route have not been delivered or marked failed yet."
							.formatted(outstanding));
		}
	}

	private DeliveryRoute requireOwnRoute(Long routeId, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		return deliveryRouteRepository.findByIdAndSupplier(routeId, supplier)
				.orElseThrow(() -> new ResourceNotFoundException("Delivery route", routeId));
	}

	private RouteResponse toRouteResponse(DeliveryRoute route) {
		List<RouteStopResponse> stops = deliveryRepository.findByRoute(route).stream()
				.map(stop -> new RouteStopResponse(
						stop.getDeliveryId(),
						stop.getOrder().getId(),
						stop.getOrder().getCafe().getName(),
						stop.getOrder().getCafe().getAddress(),
						stop.getStatus().name(),
						stop.getExpectedAt(),
						stop.getDeliveredAt()))
				.toList();

		return new RouteResponse(
				route.getId(),
				route.getRouteDate(),
				route.getDriverName(),
				route.getDriverPhone(),
				route.getStatus().name(),
				route.getNotes(),
				stops.size(),
				stops);
	}

	// ---------------- PRICE HISTORY ----------------

	private void recordPrice(Fish fish) {
		FishPriceHistory entry = new FishPriceHistory();
		entry.setFish(fish);
		entry.setPricePerKg(fish.getPricePerKg());
		fishPriceHistoryRepository.save(entry);
	}

	@Override
	public Page<PriceHistoryResponse> getFishPriceHistory(Long fishId, String supplierEmail, Pageable pageable) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		Fish fish = fishRepository.findById(fishId)
				.filter(f -> f.getSupplier().getId().equals(supplier.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Fish", fishId));

		return fishPriceHistoryRepository.findByFishOrderByRecordedAtDesc(fish, pageable)
				.map(h -> new PriceHistoryResponse(h.getPricePerKg(), h.getRecordedAt()));
	}

	// ---------------- DELIVERY ----------------

	/** Which moves are legal from each state; terminal states allow none. */
	private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED_DELIVERY_MOVES = Map.of(
			DeliveryStatus.SCHEDULED, Set.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.FAILED),
			DeliveryStatus.IN_TRANSIT, Set.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED),
			// A failed attempt can be retried by putting it back on the road.
			DeliveryStatus.FAILED, Set.of(DeliveryStatus.IN_TRANSIT),
			DeliveryStatus.DELIVERED, Set.of());

	@Override
	public DeliveryResponse getDelivery(Long orderId, String supplierEmail) {
		return toDeliveryResponse(requireDelivery(validateSupplierOrder(orderId, supplierEmail)));
	}

	@Override
	public DeliveryResponse updateDelivery(Long orderId, DeliveryUpdateRequest dto, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		Delivery delivery = requireDelivery(order);

		if (dto.status() != null && dto.status() != delivery.getStatus()) {
			DeliveryStatus from = delivery.getStatus();
			if (!ALLOWED_DELIVERY_MOVES.getOrDefault(from, Set.of()).contains(dto.status())) {
				throw new BusinessRuleException(
						"A delivery cannot move from %s to %s".formatted(from, dto.status()));
			}

			delivery.setStatus(dto.status());

			// Arrival time is recorded by the system rather than supplied by the
			// caller, so it cannot be backdated to hide a late delivery.
			if (dto.status() == DeliveryStatus.DELIVERED) {
				delivery.setDeliveredAt(LocalDateTime.now());
			}
		}

		if (dto.driverName() != null) {
			delivery.setDriverName(dto.driverName());
		}
		if (dto.driverPhone() != null) {
			delivery.setDriverPhone(dto.driverPhone());
		}
		if (dto.expectedAt() != null) {
			delivery.setExpectedAt(dto.expectedAt());
		}
		if (dto.notes() != null) {
			delivery.setNotes(dto.notes());
		}

		Delivery saved = deliveryRepository.save(delivery);

		events.publishEvent(new NotificationEvents.DeliveryStatusChanged(
				order.getId(),
				order.getCafe().getEmail(),
				saved.getStatus().name(),
				saved.getDriverName(),
				saved.getDriverPhone()));

		return toDeliveryResponse(saved);
	}

	private void publishOrderStatus(Order order) {
		events.publishEvent(new NotificationEvents.OrderStatusChanged(
				order.getId(),
				order.getCafe().getEmail(),
				order.getCafe().getName(),
				order.getSupplier().getName(),
				order.getStatus().name()));
	}

	private Delivery requireDelivery(Order order) {
		return deliveryRepository.findByOrder(order)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No delivery has been scheduled for order", order.getId()));
	}

	private DeliveryResponse toDeliveryResponse(Delivery delivery) {
		return new DeliveryResponse(
				delivery.getDeliveryId(),
				delivery.getOrder().getId(),
				delivery.getStatus().name(),
				delivery.getDriverName(),
				delivery.getDriverPhone(),
				delivery.getExpectedAt(),
				delivery.getDeliveredAt(),
				delivery.getNotes());
	}

	// ---------------- DAILY SUPPLY ----------------

	@Override
	public DailySupplyResponse addDailySupply(DailySupplyCreateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		FishType fishType = fishTypeRepository.findByNameIgnoreCase(dto.fishTypeName())
				.orElseThrow(() -> new ResourceNotFoundException("Fish type", dto.fishTypeName()));

		if (dto.catchDateTime().isAfter(LocalDateTime.now())) {
			throw new BusinessRuleException("Catch date and time cannot be in the future");
		}

		// Landing a catch is what makes fish sellable, so it credits the listing.
		// Fish.availableKg is the single stock ledger; a DailySupply row is the
		// dated intake record behind it, carrying the freshness the matcher ranks
		// on. Keeping two independent quantities meant the spot market and the
		// matching engine could each believe they had stock the other had sold.
		Fish listing = fishRepository.findBySupplierAndFishType(supplier, fishType)
				.orElseThrow(() -> new BusinessRuleException(
						"Create a %s listing before recording a catch - the catch is added to it."
								.formatted(fishType.getName())));
		listing.setAvailableKg(listing.getAvailableKg() + dto.quantity());
		fishRepository.save(listing);

		DailySupply supply = new DailySupply();
		supply.setSupplier(supplier);
		supply.setFishType(fishType);
		supply.setQuantity(dto.quantity());
		supply.setCatchDateTime(dto.catchDateTime());
		supply.setFreshnessScore(dto.freshnessScore());
		supply.setStatus(SupplyStatus.AVAILABLE);

		DailySupply saved = dailySupplyRepository.save(supply);

		// Match straight away rather than leaving the new catch idle until the
		// scheduler's next 10-minute pass - freshness is the whole point here.
		demandMatchingScheduler.autoMatchDemands();

		return toDailySupplyResponse(saved);
	}

	@Override
	public List<DailySupplyResponse> getMyDailySupply(String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		return dailySupplyRepository.findBySupplierOrderByCatchDateTimeDesc(supplier)
				.stream()
				.map(this::toDailySupplyResponse)
				.collect(Collectors.toList());
	}

	@Override
	public DailySupplyResponse updateDailySupply(Long id, DailySupplyUpdateRequest dto, String supplierEmail) {
		DailySupply supply = requireOwnDailySupply(id, supplierEmail);

		if (supply.getStatus() == SupplyStatus.EXHAUSTED) {
			throw new BusinessRuleException("Exhausted supply can no longer be edited");
		}

		if (dto.quantity() != null) {
			// Quantity already promised to accepted matches cannot be revoked.
			double committed = committedQuantity(supply);
			if (dto.quantity() < committed) {
				throw new BusinessRuleException(
						"Cannot reduce quantity below the %.1f kg already accepted against this supply"
								.formatted(committed));
			}

			// Correcting the intake has to move the ledger with it, or the two
			// drift apart again.
			double delta = dto.quantity() - supply.getQuantity();
			Fish listing = requireListingFor(supply);
			if (listing.getAvailableKg() + delta < 0) {
				throw new BusinessRuleException(
						"Reducing this catch by %.1f kg would take the %s listing below zero - %.1f kg has already been sold."
								.formatted(-delta, listing.getName(), listing.getAvailableKg()));
			}
			listing.setAvailableKg(listing.getAvailableKg() + delta);
			fishRepository.save(listing);

			supply.setQuantity(dto.quantity());
			supply.setStatus(dto.quantity() == 0 ? SupplyStatus.EXHAUSTED : SupplyStatus.AVAILABLE);
		}

		if (dto.freshnessScore() != null) {
			supply.setFreshnessScore(dto.freshnessScore());
		}

		return toDailySupplyResponse(dailySupplyRepository.save(supply));
	}

	@Override
	public void deleteDailySupply(Long id, String supplierEmail) {
		DailySupply supply = requireOwnDailySupply(id, supplierEmail);

		List<SupplyMatch> matches = supplyMatchRepository.findByDailySupply(supply);

		boolean hasAccepted = matches.stream().anyMatch(m -> m.getStatus() == MatchStatus.ACCEPTED);
		if (hasAccepted) {
			throw new BusinessRuleException(
					"This supply has been accepted against a demand and can no longer be deleted");
		}

		// Removing an intake record takes its quantity back off the ledger.
		Fish listing = requireListingFor(supply);
		if (listing.getAvailableKg() < supply.getQuantity()) {
			throw new BusinessRuleException(
					"Cannot remove this catch: only %.1f kg of the %s listing is unsold, so %.1f kg cannot be taken back."
							.formatted(listing.getAvailableKg(), listing.getName(), supply.getQuantity()));
		}
		listing.setAvailableKg(listing.getAvailableKg() - supply.getQuantity());
		fishRepository.save(listing);

		// Pending matches hold no stock, but the FK to DailySupply is uncascaded
		// so they have to go first.
		supplyMatchRepository.deleteAll(matches);
		dailySupplyRepository.delete(supply);
	}

	/** The listing a catch feeds. Guaranteed to exist - addDailySupply requires it. */
	private Fish requireListingFor(DailySupply supply) {
		return fishRepository.findBySupplierAndFishType(supply.getSupplier(), supply.getFishType())
				.orElseThrow(() -> new BusinessRuleException(
						"The %s listing this catch belongs to no longer exists."
								.formatted(supply.getFishType().getName())));
	}

	/** Quantity locked in by matches the supplier has already accepted. */
	private double committedQuantity(DailySupply supply) {
		return supplyMatchRepository.findByDailySupply(supply).stream()
				.filter(match -> match.getStatus() == MatchStatus.ACCEPTED)
				.mapToDouble(SupplyMatch::getConfirmedQuantity)
				.sum();
	}

	private DailySupply requireOwnDailySupply(Long id, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		return dailySupplyRepository.findById(id)
				.filter(supply -> supply.getSupplier().getId().equals(supplier.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Daily supply", id));
	}

	private DailySupplyResponse toDailySupplyResponse(DailySupply supply) {
		return new DailySupplyResponse(
				supply.getId(),
				supply.getFishType().getId(),
				supply.getFishType().getName(),
				supply.getQuantity(),
				supply.getCatchDateTime(),
				supply.getFreshnessScore(),
				supply.getStatus().name()
		);
	}

	/**
	 * Loads a match only if it belongs to the calling supplier's own daily supply.
	 * 404 rather than 403 on a mismatch: a 403 would confirm the id exists and let
	 * one supplier enumerate another's matches.
	 */
	private SupplyMatch requireOwnMatch(Long matchId, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierEmail));

		return supplyMatchRepository.findById(matchId)
				.filter(match -> match.getDailySupply().getSupplier().getId().equals(supplier.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Supply match", matchId));
	}

	@Override
	public void acceptMatch(Long id, String supplierEmail) {

		SupplyMatch match = requireOwnMatch(id, supplierEmail);

	    if (match.getStatus() != MatchStatus.PENDING) {
	        throw new BusinessRuleException("Match already processed");
	    }

	    DailySupply supply = match.getDailySupply();

	    // The intake record is drawn down alongside the listing so the two stay in
	    // step: createOrderFromMatch below reserves the same quantity on the Fish
	    // ledger, and stock landed by this catch is no longer offerable from it.
	    double remainingQty = supply.getQuantity() - match.getConfirmedQuantity();
	    supply.setQuantity(Math.max(0, remainingQty));

	    if (supply.getQuantity() <= 0) {
	        supply.setStatus(SupplyStatus.EXHAUSTED);
	    }

	    dailySupplyRepository.save(supply);

	    match.setStatus(MatchStatus.ACCEPTED);
	    supplyMatchRepository.save(match);

	    // Create order
	    createOrderFromMatch(match);
		
	}
	
	private void createOrderFromMatch(SupplyMatch match) {

	    DailySupply supply = match.getDailySupply();
	    Supplier supplier = supply.getSupplier();
	    FishType fishType = match.getDemandRequest().getFishType();

	    // 🔥 Find supplier's Fish by FishType
	    Fish fish = fishRepository
	            .findBySupplierAndFishType(supplier, fishType)
	            .orElseThrow(() -> new BusinessRuleException(
	                    "You have no %s listing to fulfil this match from. Create one first."
	                            .formatted(fishType.getName())));

	    double quantity = match.getConfirmedQuantity();

	    // The order references a Fish listing, so the listing has to be reserved
	    // exactly as a spot-market order would. Skipping this let the same kilos
	    // stay on sale after being promised to a demand match, and left the
	    // reservation counter to go negative when the order was later accepted.
	    if (fish.getAvailableKg() < quantity) {
	        throw new BusinessRuleException(
	                "Your %s listing has only %.1f kg available but this match needs %.1f kg. Restock before accepting."
	                        .formatted(fish.getName(), fish.getAvailableKg(), quantity));
	    }
	    fish.setAvailableKg(fish.getAvailableKg() - quantity);
	    fish.setReservedKg(fish.getReservedKg() + quantity);

	    Order order = new Order();
	    order.setCafe(match.getDemandRequest().getCafe());
	    order.setSupplier(supplier);
	    order.setStatus(OrderStatus.REQUESTED);
	    // Keeps the trail from order back to the match and the demand behind it.
	    order.setSupplyMatch(match);

	    OrderItem item = new OrderItem();
	    item.setOrder(order);
	    item.setFish(fish);
	    item.setQuantityKg(quantity);
	    item.setPricePerKg(fish.getPricePerKg());

	    order.setItems(List.of(item));
	    orderRepository.save(order);
	}

	@Override
	public void rejectMatch(Long id, String supplierEmail) {
		SupplyMatch match = requireOwnMatch(id, supplierEmail);

		// Without this, rejecting an already-accepted match would reopen the demand
		// while the order created on accept still stands.
		if (match.getStatus() != MatchStatus.PENDING) {
			throw new BusinessRuleException("Match already processed");
		}

	    match.setStatus(MatchStatus.REJECTED);
	    supplyMatchRepository.save(match);

	    // Recompute rather than forcing OPEN: other suppliers may still cover part
	    // or all of this demand, and flatly reopening it misreported a demand that
	    // was in fact still partially or fully matched.
	    demandMatchService.applyStatus(match.getDemandRequest());
	}
}
