package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.model.Cafe;

public interface CafeRepository extends JpaRepository<Cafe, Long>{

	Optional<Cafe> findByEmail(String email);

}
