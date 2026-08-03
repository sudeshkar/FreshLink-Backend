package com.freshlink.service.interfaces.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.FishPriceHistoryRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.marketdto.MarketPriceSummary;
import com.freshlink.marketdto.PriceTrend;
import com.freshlink.marketdto.PriceTrendPoint;
import com.freshlink.model.FishType;
import com.freshlink.service.interfaces.MarketService;

import lombok.RequiredArgsConstructor;

/**
 * Market-wide pricing, for both sides of the trade: a cafe judging whether an
 * offer is fair, and a supplier deciding what to charge.
 *
 * Per-listing history already existed, but on its own it only answers "what has
 * this one supplier charged" - not "what does this fish cost".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketServiceImpl implements MarketService {

	/** A year of daily points is already a lot to render; beyond that it is noise. */
	private static final int MAX_WINDOW_DAYS = 365;

	private final FishRepository fishRepository;
	private final FishPriceHistoryRepository fishPriceHistoryRepository;
	private final FishTypeRepository fishTypeRepository;

	@Override
	public List<MarketPriceSummary> getPriceSummary() {
		LocalDate today = LocalDate.now();

		return fishRepository.findMarketPriceSummary().stream()
				.map(row -> new MarketPriceSummary(
						(Long) row[0],
						(String) row[1],
						money(row[2]),
						money(row[3]),
						money(row[4]),
						((Number) row[5]).longValue(),
						((Number) row[6]).doubleValue(),
						inSeason((LocalDate) row[7], (LocalDate) row[8], today)))
				.toList();
	}

	@Override
	public PriceTrend getPriceTrend(Long fishTypeId, int days) {
		if (days < 1 || days > MAX_WINDOW_DAYS) {
			throw new BusinessRuleException(
					"Window must be between 1 and %d days".formatted(MAX_WINDOW_DAYS));
		}

		FishType fishType = fishTypeRepository.findById(fishTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("Fish type", fishTypeId));

		List<PriceTrendPoint> points = fishPriceHistoryRepository
				.findDailyAveragePrices(fishTypeId, LocalDateTime.now().minusDays(days))
				.stream()
				.map(row -> new PriceTrendPoint(
						toLocalDate(row[0]),
						money(row[1]),
						((Number) row[2]).longValue()))
				.toList();

		return new PriceTrend(
				fishType.getId(),
				fishType.getName(),
				days,
				points.stream().map(PriceTrendPoint::averagePricePerKg)
						.min(Comparator.naturalOrder()).orElse(null),
				points.stream().map(PriceTrendPoint::averagePricePerKg)
						.max(Comparator.naturalOrder()).orElse(null),
				points);
	}

	/** Averages come back with full division scale; money needs two places. */
	private BigDecimal money(Object value) {
		return value == null ? null
				: new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * The JPA provider may hand back java.sql.Date or LocalDate for a grouped
	 * date expression, so this accepts either rather than assuming.
	 */
	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate date) {
			return date;
		}
		if (value instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		return LocalDate.parse(value.toString());
	}

	private boolean inSeason(LocalDate start, LocalDate end, LocalDate today) {
		if (start == null || end == null) {
			return true;
		}
		// Compared on month and day: a season is an annual window, and the seeded
		// years would otherwise make everything permanently out of season.
		int now = today.getMonthValue() * 100 + today.getDayOfMonth();
		int from = start.getMonthValue() * 100 + start.getDayOfMonth();
		int to = end.getMonthValue() * 100 + end.getDayOfMonth();

		// A season that wraps the new year, such as November to February.
		return from <= to ? now >= from && now <= to : now >= from || now <= to;
	}
}
