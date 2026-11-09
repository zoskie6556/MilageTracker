package com.fueltrack.service;

import com.fueltrack.dto.DailyMileageSummary;
import com.fueltrack.dto.MonthlyMileageSummary;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.OdometerReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class MileageSummaryService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Africa/Gaborone");

    private final OdometerReadingRepository readingRepository;

    public MileageSummaryService(OdometerReadingRepository readingRepository) {
        this.readingRepository = readingRepository;
    }

    /**
     * Summarizes daily and monthly distances for all readings owned by the user.
     *
     * @param userId authenticated owner
     * @return daily and monthly totals in Gaborone local time
     */
    @Transactional(readOnly = true)
    public MileageSummary summarize(Long userId) {
        List<OdometerReading> readings = readingRepository.findAllOwnedByUserIdOrderByTakenAtDesc(userId);
        Map<Long, List<OdometerReading>> byVehicle = readings.stream()
                .collect(Collectors.groupingBy(reading -> reading.getVehicle().getId()));
        List<DailyMileageSummary> daily = new ArrayList<>();
        Map<VehicleMonth, BigDecimal> monthDistances = new HashMap<>();
        Map<VehicleMonth, String> vehicleNames = new HashMap<>();

        byVehicle.forEach((vehicleId, vehicleReadings) -> {
            Map<LocalDate, List<OdometerReading>> byDate = vehicleReadings.stream()
                    .collect(Collectors.groupingBy(reading ->
                            reading.getTakenAt().atZone(DISPLAY_ZONE).toLocalDate(), TreeMap::new,
                            Collectors.toList()));
            for (Map.Entry<LocalDate, List<OdometerReading>> entry : byDate.entrySet()) {
                List<OdometerReading> onDay = entry.getValue();
                onDay.sort(Comparator.comparing(OdometerReading::getTakenAt));
                OdometerReading first = onDay.getFirst();
                OdometerReading last = onDay.getLast();
                BigDecimal distance = last.getReadingKm().subtract(first.getReadingKm());
                String vehicleName = first.getVehicle().getName();
                daily.add(new DailyMileageSummary(
                        vehicleId, vehicleName, entry.getKey(),
                        first.getReadingKm(), last.getReadingKm(), distance));
                VehicleMonth key = new VehicleMonth(vehicleId, YearMonth.from(entry.getKey()));
                monthDistances.merge(key, distance, BigDecimal::add);
                vehicleNames.put(key, vehicleName);
            }
        });

        daily.sort(Comparator.comparing(DailyMileageSummary::day).reversed()
                .thenComparing(DailyMileageSummary::vehicleName));
        List<MonthlyMileageSummary> monthly = monthDistances.entrySet().stream()
                .map(entry -> new MonthlyMileageSummary(
                        entry.getKey().vehicleId(), vehicleNames.get(entry.getKey()),
                        entry.getKey().month(), entry.getValue()))
                .sorted(Comparator.comparing(MonthlyMileageSummary::month).reversed()
                        .thenComparing(MonthlyMileageSummary::vehicleName))
                .toList();
        return new MileageSummary(List.copyOf(daily), monthly);
    }

    private record VehicleMonth(Long vehicleId, YearMonth month) {
    }

    public record MileageSummary(List<DailyMileageSummary> daily, List<MonthlyMileageSummary> monthly) {
    }
}
