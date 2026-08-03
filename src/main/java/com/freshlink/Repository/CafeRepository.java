package com.freshlink.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Cafe;

public interface CafeRepository extends JpaRepository<Cafe, Long>{

	Optional<Cafe> findByEmail(String email);

	/** Live cafes only - soft-deleted accounts must not appear in listings. */
	Page<Cafe> findByDeletedAtIsNull(Pageable pageable);

}
