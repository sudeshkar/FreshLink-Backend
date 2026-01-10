package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.FishType;

public interface FishTypeRepository extends JpaRepository<FishType, Long> {
	Optional<FishType> findByNameIgnoreCase(String name);
}
