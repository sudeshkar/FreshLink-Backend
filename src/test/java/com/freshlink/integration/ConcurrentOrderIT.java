package com.freshlink.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderItemDto;
import com.freshlink.service.interfaces.CafeService;

/**
 * Stock is reserved by reading a Fish, decrementing it, and writing it back.
 * Two orders arriving together can therefore both read the same starting
 * quantity and both believe there is enough - the classic lost update, which
 * oversells the supplier.
 *
 * Fish carries a @Version column to prevent that, but nothing exercised it:
 * every other test mocks the repository, so the locking was effectively
 * untested. These run real concurrent transactions against the real database.
 */
@SpringBootTest(properties = {
		"app.ratelimit.enabled=false",
		"app.notifications.enabled=false"
})
@ActiveProfiles("dev")
class ConcurrentOrderIT {

	@Autowired private CafeService cafeService;
	@Autowired private FishRepository fishRepository;
	@Autowired private FishTypeRepository fishTypeRepository;
	@Autowired private SupplierRepository supplierRepository;

	private static final String CAFE = "cafe1@freshlink.com";

	/**
	 * A listing of its own per test, so contention is genuinely between this
	 * test's threads and not with whatever else has touched the seeded data.
	 */
	private Fish freshListing(String fishTypeName, double availableKg) {
		Supplier supplier = supplierRepository.findByEmail("supplier1@freshlink.com").orElseThrow();
		FishType type = fishTypeRepository.findByNameIgnoreCase(fishTypeName).orElseThrow();

		Fish fish = new Fish();
		fish.setSupplier(supplier);
		fish.setFishType(type);
		fish.setName("Contention " + fishTypeName + " " + System.nanoTime());
		fish.setPricePerKg(BigDecimal.valueOf(1000));
		fish.setAvailableKg(availableKg);
		fish.setReservedKg(0);
		return fishRepository.saveAndFlush(fish);
	}

	/** Fires every task at once so they collide rather than queue politely. */
	private List<Outcome> raceOrders(Long fishId, double quantityEach, int threads) throws Exception {
		CountDownLatch startGun = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		List<Future<Outcome>> futures = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			Callable<Outcome> task = () -> {
				startGun.await();
				try {
					cafeService.placeOrder(
							new OrderCreateRequest(List.of(new OrderItemDto(fishId, quantityEach))),
							CAFE);
					return new Outcome(true, null);
				} catch (Exception e) {
					return new Outcome(false, e.getClass().getSimpleName());
				}
			};
			futures.add(pool.submit(task));
		}

		startGun.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(30, TimeUnit.SECONDS))
				.as("orders should not deadlock")
				.isTrue();

		List<Outcome> outcomes = new ArrayList<>();
		for (Future<Outcome> future : futures) {
			outcomes.add(future.get());
		}
		return outcomes;
	}

	private record Outcome(boolean succeeded, String failure) {
	}

	@Test
	@DisplayName("two orders that cannot both fit: exactly one wins, and stock is never oversold")
	void concurrentOrdersCannotOversell() throws Exception {
		Fish fish = freshListing("Crab", 100.0);

		// 60 + 60 against 100kg. Whichever way the two interleave, only one can be
		// satisfied: either the loser hits the version conflict, or it re-reads the
		// reduced quantity and is refused for insufficient stock.
		List<Outcome> outcomes = raceOrders(fish.getId(), 60.0, 2);

		long succeeded = outcomes.stream().filter(Outcome::succeeded).count();
		assertThat(succeeded)
				.as("exactly one of two competing 60kg orders may succeed against 100kg, got %s", outcomes)
				.isEqualTo(1);

		Fish after = fishRepository.findById(fish.getId()).orElseThrow();
		assertThat(after.getAvailableKg()).isEqualTo(40.0);
		assertThat(after.getReservedKg()).isEqualTo(60.0);
	}

	@Test
	@DisplayName("many concurrent orders that all fit: no reservation is lost")
	void concurrentOrdersDoNotLoseUpdates() throws Exception {
		double startingStock = 100.0;
		double each = 5.0;
		int threads = 8;

		Fish fish = freshListing("Mackerel", startingStock);

		List<Outcome> outcomes = raceOrders(fish.getId(), each, threads);
		long succeeded = outcomes.stream().filter(Outcome::succeeded).count();

		// Some losing to the version check is expected and fine - that is the lock
		// doing its job. What must hold is that the books balance exactly: every
		// order that reported success is reflected in the stock, and none vanished.
		Fish after = fishRepository.findById(fish.getId()).orElseThrow();

		assertThat(after.getReservedKg())
				.as("reserved must equal exactly the orders that succeeded")
				.isEqualTo(succeeded * each);

		assertThat(after.getAvailableKg() + after.getReservedKg())
				.as("available + reserved must still account for all original stock")
				.isEqualTo(startingStock);

		assertThat(succeeded).as("at least one order should get through").isPositive();
	}

	@Test
	@DisplayName("a losing order fails cleanly, without partially reserving stock")
	void losingOrderLeavesNoResidue() throws Exception {
		Fish fish = freshListing("Salmon", 10.0);

		AtomicInteger conflicts = new AtomicInteger();
		List<Outcome> outcomes = raceOrders(fish.getId(), 8.0, 3);
		outcomes.stream().filter(o -> !o.succeeded()).forEach(o -> conflicts.incrementAndGet());

		assertThat(outcomes.stream().filter(Outcome::succeeded).count()).isEqualTo(1);
		assertThat(conflicts.get()).isEqualTo(2);

		Fish after = fishRepository.findById(fish.getId()).orElseThrow();
		assertThat(after.getAvailableKg())
				.as("the two failures must not have nibbled any stock")
				.isEqualTo(2.0);
		assertThat(after.getReservedKg()).isEqualTo(8.0);
	}
}
