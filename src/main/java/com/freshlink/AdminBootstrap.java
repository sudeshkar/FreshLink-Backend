package com.freshlink;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.DailySupplyRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.enums.Role;
import com.freshlink.enums.SupplyStatus;
import com.freshlink.model.Admin;
import com.freshlink.model.Cafe;
import com.freshlink.model.DailySupply;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Bootstraps the initial admin account and reference data, and seeds demo
 * traders for local development.
 *
 * Disabled unless {@code app.bootstrap.enabled=true}, which only the dev
 * profile sets, so the demo data can never reach a production database.
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AdminBootstrap {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final FishTypeRepository fishTypeRepository;
	private final SupplierRepository supplierRepository;
	private final CafeRepository cafeRepository;
	private final FishRepository fishRepository;
	private final DailySupplyRepository dailySupplyRepository;

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
	public void seed() {
		seedAdmin();
		seedFishTypes();
		seedUsers();
		seedFishInventory();
		seedDailySupply();

		log.info("Demo data seeding complete");
	}

	// ---------------- ADMIN ----------------

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

	// ---------------- FISH TYPES ----------------

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

	// ---------------- DEMO TRADERS ----------------

	private void seedUsers() {

		if (supplierRepository.count() == 0) {
			Supplier s1 = new Supplier();
			s1.setName("Ocean Fresh Suppliers");
			s1.setEmail("supplier1@freshlink.com");
			s1.setPhone("0711111111");
			s1.setPasswordHash(passwordEncoder.encode("password"));
			s1.setRole(Role.SUPPLIER);
			s1.setActive(true);
			s1.setEmailVerified(true);
			s1.setLocation("Negombo");
			s1.setLicenseNumber("LIC-NEG-001");
			s1.setAverageRating(0.0);
			s1.setRatingCount(0);

			Supplier s2 = new Supplier();
			s2.setName("Sea Harvest Pvt Ltd");
			s2.setEmail("supplier2@freshlink.com");
			s2.setPhone("0722222222");
			s2.setPasswordHash(passwordEncoder.encode("password"));
			s2.setRole(Role.SUPPLIER);
			s2.setActive(true);
			s2.setEmailVerified(true);
			s2.setLocation("Trincomalee");
			s2.setLicenseNumber("LIC-TRI-002");
			s2.setAverageRating(0.0);
			s2.setRatingCount(0);

			supplierRepository.saveAll(List.of(s1, s2));
		}

		if (cafeRepository.count() == 0) {
			Cafe c1 = new Cafe();
			c1.setName("Cafe Blue Wave");
			c1.setEmail("cafe1@freshlink.com");
			c1.setPhone("0777777777");
			c1.setPasswordHash(passwordEncoder.encode("password"));
			c1.setRole(Role.CAFE);
			c1.setActive(true);
			c1.setEmailVerified(true);
			c1.setAddress("888 Main Street, CircularZone");
			c1.setBusinessRegNo("4556582");

			Cafe c2 = new Cafe();
			c2.setName("Ocean Bite Cafe");
			c2.setEmail("cafe2@freshlink.com");
			c2.setPhone("0788888888");
			c2.setPasswordHash(passwordEncoder.encode("password"));
			c2.setRole(Role.CAFE);
			c2.setActive(true);
			c2.setEmailVerified(true);
			c2.setAddress("123 Main Street, Springfield");
			c2.setBusinessRegNo("45a4d54");

			cafeRepository.saveAll(List.of(c1, c2));
		}
	}

	// ---------------- FISH INVENTORY ----------------

	private void seedFishInventory() {

		if (fishRepository.count() > 0) {
			return;
		}

		Supplier supplier1 = supplierRepository.findByEmail("supplier1@freshlink.com").orElseThrow();
		Supplier supplier2 = supplierRepository.findByEmail("supplier2@freshlink.com").orElseThrow();

		FishType tuna = fishTypeRepository.findByNameIgnoreCase("Tuna").orElseThrow();
		FishType prawns = fishTypeRepository.findByNameIgnoreCase("Prawns").orElseThrow();
		FishType salmon = fishTypeRepository.findByNameIgnoreCase("Salmon").orElseThrow();

		// Trailing nulls are createdAt/updatedAt - Hibernate fills them in.
		fishRepository.saveAll(List.of(
			new Fish(null, null, tuna, "Fresh Tuna", BigDecimal.valueOf(1800), 100, 0, supplier1, null, null),
			new Fish(null, null, prawns, "King Prawns", BigDecimal.valueOf(2200), 80, 0, supplier1, null, null),
			new Fish(null, null, salmon, "Atlantic Salmon", BigDecimal.valueOf(2500), 60, 0, supplier2, null, null)
		));
	}

	// ---------------- DAILY SUPPLY ----------------

	private void seedDailySupply() {

		if (dailySupplyRepository.count() > 0) {
			return;
		}

		Supplier supplier1 = supplierRepository.findByEmail("supplier1@freshlink.com").orElseThrow();
		Supplier supplier2 = supplierRepository.findByEmail("supplier2@freshlink.com").orElseThrow();

		FishType tuna = fishTypeRepository.findByNameIgnoreCase("Tuna").orElseThrow();
		FishType salmon = fishTypeRepository.findByNameIgnoreCase("Salmon").orElseThrow();

		dailySupplyRepository.saveAll(List.of(
			new DailySupply(null, supplier1, tuna, 50.0,
				LocalDateTime.now().minusHours(3), SupplyStatus.AVAILABLE, 0.9),

			new DailySupply(null, supplier2, salmon, 40.0,
				LocalDateTime.now().minusHours(6), SupplyStatus.AVAILABLE, 0.85)
		));
	}

}
