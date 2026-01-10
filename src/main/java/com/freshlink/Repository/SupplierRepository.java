package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long>{

	Optional<Supplier> findByEmail(String supplierEmail);

}
