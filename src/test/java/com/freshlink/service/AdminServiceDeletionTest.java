package com.freshlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.enums.Role;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.model.Admin;
import com.freshlink.model.Cafe;
import com.freshlink.service.interfaces.impl.AdminServiceImpl;

/**
 * deleteUser was a hard deleteById: it would violate the foreign keys held by
 * orders and ratings, erase trade history, and happily remove the last admin
 * or the caller's own account.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceDeletionTest {

	@Mock private SupplierRepository supplierRepository;
	@Mock private CafeRepository cafeRepository;
	@Mock private UserRepository userRepository;
	@Mock private OrderRepository orderRepository;

	@InjectMocks private AdminServiceImpl adminService;

	private Admin admin(Long id, String email) {
		Admin admin = new Admin();
		admin.setId(id);
		admin.setEmail(email);
		admin.setRole(Role.ADMIN);
		return admin;
	}

	private Cafe cafe(Long id, String email) {
		Cafe cafe = new Cafe();
		cafe.setId(id);
		cafe.setEmail(email);
		cafe.setRole(Role.CAFE);
		return cafe;
	}

	@Test
	@DisplayName("an admin cannot delete their own account")
	void refusesSelfDeletion() {
		Admin self = admin(1L, "admin@freshlink.test");
		when(userRepository.findById(1L)).thenReturn(Optional.of(self));

		assertThatThrownBy(() -> adminService.deleteUser(1L, "admin@freshlink.test"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("your own");

		assertThat(self.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("the last remaining admin cannot be deleted, which would lock everyone out")
	void refusesDeletingLastAdmin() {
		Admin other = admin(2L, "other@freshlink.test");
		when(userRepository.findById(2L)).thenReturn(Optional.of(other));
		when(userRepository.countByRoleAndDeletedAtIsNull(Role.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> adminService.deleteUser(2L, "admin@freshlink.test"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("last remaining admin");

		assertThat(other.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("an account with orders still in progress cannot be deleted")
	void refusesDeletingAccountWithInFlightOrders() {
		Cafe cafe = cafe(3L, "cafe@freshlink.test");
		when(userRepository.findById(3L)).thenReturn(Optional.of(cafe));
		when(orderRepository.countByCafeAndStatusIn(eq(cafe), anyList())).thenReturn(2L);

		assertThatThrownBy(() -> adminService.deleteUser(3L, "admin@freshlink.test"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("in progress");

		assertThat(cafe.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("deletion is a soft delete that preserves the row for order history")
	void softDeletesRatherThanRemovingTheRow() {
		Cafe cafe = cafe(3L, "cafe@freshlink.test");
		when(userRepository.findById(3L)).thenReturn(Optional.of(cafe));
		when(orderRepository.countByCafeAndStatusIn(eq(cafe), anyList())).thenReturn(0L);

		adminService.deleteUser(3L, "admin@freshlink.test");

		assertThat(cafe.getDeletedAt()).isNotNull();
		assertThat(cafe.isActive()).isFalse();
		verify(userRepository).save(cafe);
		verify(userRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("an already-deleted account is invisible to further admin actions")
	void softDeletedUserIsNotFound() {
		Cafe cafe = cafe(3L, "cafe@freshlink.test");
		cafe.setDeletedAt(java.time.LocalDateTime.now());
		when(userRepository.findById(3L)).thenReturn(Optional.of(cafe));

		assertThatThrownBy(() -> adminService.activateUser(3L))
				.isInstanceOf(com.freshlink.exception.ResourceNotFoundException.class);
	}
}
