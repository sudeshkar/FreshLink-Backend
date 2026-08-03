package com.freshlink.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.DeliveryRoute;
import com.freshlink.model.Supplier;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, Long>{

	Page<DeliveryRoute> findBySupplierOrderByRouteDateDesc(Supplier supplier, Pageable pageable);

	Optional<DeliveryRoute> findByIdAndSupplier(Long id, Supplier supplier);
}
