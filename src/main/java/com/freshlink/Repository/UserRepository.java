package com.freshlink.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freshlink.enums.Role;
import com.freshlink.model.User;

public interface UserRepository extends JpaRepository<User, Long>{

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	/** Used to refuse deletion of the last surviving admin. */
	long countByRoleAndDeletedAtIsNull(Role role);
	
 

}
