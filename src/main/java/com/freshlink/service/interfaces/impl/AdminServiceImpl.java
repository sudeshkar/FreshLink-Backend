package com.freshlink.service.interfaces.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.admindto.CafeAdminResponse;
import com.freshlink.admindto.SupplierAdminResponse;
import com.freshlink.enums.OrderStatus;
import com.freshlink.enums.Role;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.model.Cafe;
import com.freshlink.model.Supplier;
import com.freshlink.model.User;
import com.freshlink.service.interfaces.AdminService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

	private final SupplierRepository supplierRepository;
    private final CafeRepository cafeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    
	/** Orders that still need someone to act on them. */
	private static final List<OrderStatus> IN_FLIGHT_ORDER_STATUSES =
			List.of(OrderStatus.REQUESTED, OrderStatus.ACCEPTED, OrderStatus.DELIVERING);

	@Override
	public List<SupplierAdminResponse> getSuppliers() {
		 return supplierRepository.findByDeletedAtIsNull()
	                .stream()
	                .map(s -> new SupplierAdminResponse(s.getId(), s.getName(), s.getEmail(), s.isActive(), s.getPhone()))
	                .collect(Collectors.toList());
	}

	@Override
	public List<CafeAdminResponse> getCafes() {
		return cafeRepository.findByDeletedAtIsNull()
                .stream()
                .map(c -> new CafeAdminResponse(c.getId(), c.getName(), c.getEmail(), c.isActive(), c.getPhone()))
                .collect(Collectors.toList());
	}

	@Override
	public void activateUser(Long userId) {
		User user = requireLiveUser(userId);
		user.setActive(true);
		userRepository.save(user);
	}

	@Override
	public void deactivateUser(Long userId) {
		User user = requireLiveUser(userId);
		user.setActive(false);
		userRepository.save(user);
	}

	@Override
	public void deleteUser(Long userId, String actingAdminEmail) {
		User user = requireLiveUser(userId);

		if (user.getEmail().equals(actingAdminEmail)) {
			throw new BusinessRuleException("You cannot delete your own admin account");
		}

		if (user.getRole() == Role.ADMIN
				&& userRepository.countByRoleAndDeletedAtIsNull(Role.ADMIN) <= 1) {
			throw new BusinessRuleException("Cannot delete the last remaining admin account");
		}

		long inFlight = countInFlightOrders(user);
		if (inFlight > 0) {
			throw new BusinessRuleException(
					"This account has %d order(s) still in progress. Resolve them before deleting."
							.formatted(inFlight));
		}

		// Soft delete: orders, ratings and analytics all reference this row. A hard
		// delete would either violate those foreign keys or erase trade history.
		// The email is deliberately kept, so a removed account cannot re-register
		// under the same address to shed its rating history.
		user.setDeletedAt(LocalDateTime.now());
		user.setActive(false);
		userRepository.save(user);
	}

	private User requireLiveUser(Long userId) {
		return userRepository.findById(userId)
				.filter(u -> u.getDeletedAt() == null)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

	private long countInFlightOrders(User user) {
		if (user instanceof Cafe cafe) {
			return orderRepository.countByCafeAndStatusIn(cafe, IN_FLIGHT_ORDER_STATUSES);
		}
		if (user instanceof Supplier supplier) {
			return orderRepository.countBySupplierAndStatusIn(supplier, IN_FLIGHT_ORDER_STATUSES);
		}
		return 0;
	}

}
