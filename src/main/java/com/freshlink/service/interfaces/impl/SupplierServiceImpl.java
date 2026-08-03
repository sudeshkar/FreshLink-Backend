package com.freshlink.service.interfaces.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.DailySupplyRepository;
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
import com.freshlink.service.interfaces.DemandMatchService;
import com.freshlink.service.interfaces.DemandMatchingScheduler;
import com.freshlink.service.interfaces.SupplierService;
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
    
	@Override
	
	public FishResponse addFish(FishCreateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
		FishType fishType = fishTypeRepository.findByNameIgnoreCase(dto.fishTypeName())
	            .orElseThrow(() -> new RuntimeException("Fish type not found"));
		
		if (fishRepository.existsBySupplierAndFishType(supplier, fishType)) {
		    throw new RuntimeException("Fish already exists for supplier");
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
	                .orElseThrow(() -> new RuntimeException("Supplier not found"));
	        return fishRepository.findBySupplier(supplier)
	                .stream()
	                .map(fishMapper::toFishResponse) 
	                .collect(Collectors.toList());
	}
	@Override
	public FishResponse updateFish(Long id, FishUpdateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
		
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
                    .orElseThrow(() -> new RuntimeException("Fish type not found"));

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
	                .orElseThrow(() -> new RuntimeException("Supplier not found"));

	        Fish fish = fishRepository.findById(id)
	                .filter(f -> f.getSupplier().getId().equals(supplier.getId()))
	                .orElseThrow(() -> new ResourceNotFoundException("Fish", id));

	        fishRepository.delete(fish);
		
	}
	
	@Override
	public SupplierProfileResponse getProfile(String name) {
		 Supplier supplier = supplierRepository.findByEmail(name)
				 .orElseThrow(() -> new RuntimeException("Supplier not found"));
		 
		 return supplierMapper.toProfile(supplier);
	}
	
	@Override
	public OrderResponse acceptOrder(Long orderId, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		
		if (order.getStatus() != OrderStatus.REQUESTED) {
	        throw new RuntimeException("Order cannot be accepted");
	    }
		
		for (OrderItem item : order.getItems()) {
	        Fish fish = item.getFish();

	        fish.setReservedKg(
	            fish.getReservedKg() - item.getQuantityKg()
	        );
	         
	    }
		 order.setStatus(OrderStatus.ACCEPTED);
		 
		 
		  
		 
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
		        throw new RuntimeException("Order cannot be rejected");
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

		  
		
	}
	@Override
	public void markDelivering(Long orderId, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		if (order.getStatus() != OrderStatus.ACCEPTED) {
	        throw new RuntimeException("Invalid state");
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
	            .orElseThrow(() -> new RuntimeException("Supplier not found"));

	    return orderRepository.findById(orderId)
	            .filter(order -> order.getSupplier().getId().equals(supplier.getId()))
	            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
	}
	@Override
	public List<SupplyMatchResponse> getPendingMatches(String supplierEmail) {
		
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	            .orElseThrow(() -> new RuntimeException("Supplier not found"));
		
		 List<SupplyMatch> supplyMatches = supplyMatchRepository.findByDailySupply_SupplierAndStatus(supplier,MatchStatus.PENDING);
		 List<SupplyMatchResponse> responses = new ArrayList<SupplyMatchResponse>();
		 
		 for (SupplyMatch supplyMatch : supplyMatches) {
			 SupplyMatchResponse response =SupplyMatchMapper.toResponse(supplyMatch);
			 responses.add(response);
		}
		 
		 return responses;
		 
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

		return toDeliveryResponse(deliveryRepository.save(delivery));
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

		// Pending matches hold no stock, but the FK to DailySupply is uncascaded
		// so they have to go first.
		supplyMatchRepository.deleteAll(matches);
		dailySupplyRepository.delete(supply);
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

	    // Deduct quantity ONLY here
	    double remainingQty = supply.getQuantity() - match.getConfirmedQuantity();
	    supply.setQuantity(remainingQty);

	    if (remainingQty == 0) {
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
	            .orElseThrow(() -> new RuntimeException(
	                    "Fish not found for supplier and type"));

	    Order order = new Order();
	    order.setCafe(match.getDemandRequest().getCafe());
	    order.setSupplier(supplier);
	    order.setStatus(OrderStatus.REQUESTED);
	    // Keeps the trail from order back to the match and the demand behind it.
	    order.setSupplyMatch(match);

	    OrderItem item = new OrderItem();
	    item.setOrder(order);
	    item.setFish(fish); // ✅ FIXED
	    item.setQuantityKg(match.getConfirmedQuantity());
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
