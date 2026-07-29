package com.freshlink;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.enums.Role;
import com.freshlink.model.Admin;
import com.freshlink.model.FishType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrap {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final FishTypeRepository fishTypeRepository;

	@Value("${app.admin.email:}")
	private String adminEmail;

	@Value("${app.admin.password:}")
	private String adminPassword;

	@Value("${app.admin.name:System Admin}")
	private String adminName;

	@Value("${app.admin.phone:0000000000}")
	private String adminPhone;

	@PostConstruct
	@Transactional
	public void bootstrap() {
		seedAdmin();
		seedFishTypes();
	}

	/**
	 * Creates the initial admin account from configuration. Credentials are never
	 * hardcoded: if they are not supplied, bootstrapping is skipped rather than
	 * falling back to a known default.
	 */
	private void seedAdmin() {
		if (adminEmail.isBlank() || adminPassword.isBlank()) {
			log.warn("app.admin.email / app.admin.password not set - skipping admin bootstrap.");
			return;
		}

		if (userRepository.existsByEmail(adminEmail)) {
			return;
		}

		Admin admin = new Admin();
		admin.setName(adminName);
		admin.setEmail(adminEmail);
		admin.setPhone(adminPhone);
		admin.setRole(Role.ADMIN);
		admin.setPasswordHash(passwordEncoder.encode(adminPassword));
		admin.setActive(true);
		admin.setEmailVerified(true);

		userRepository.save(admin);
		log.info("Created bootstrap admin account for {}", adminEmail);
	}

	private void seedFishTypes() {
		if (fishTypeRepository.count() > 0) {
			return;
		}

		List<FishType> fishTypes = List.of(
			new FishType(null, "Tuna", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
			new FishType(null, "Salmon", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 9, 30)),
			new FishType(null, "Prawns", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 11, 30)),
			new FishType(null, "Mackerel", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 8, 31)),
			new FishType(null, "Crab", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 10, 31))
		);

		fishTypeRepository.saveAll(fishTypes);
		log.info("Seeded {} fish types", fishTypes.size());
	}
}
