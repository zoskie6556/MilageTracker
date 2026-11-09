package com.fueltrack.service;

import com.fueltrack.model.OdometerReading;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DailyDistanceCalculator {

    /**
     * Calculates each day's last reading minus its first, regardless of reading source.
     *
     * @param readings odometer readings from one vehicle
     * @param zoneId zone used to determine the local calendar day
     * @return ascending local dates and their daily distances in kilometres
     */
    public Map<LocalDate, BigDecimal> calculate(List<OdometerReading> readings, ZoneId zoneId) {
        Map<LocalDate, List<OdometerReading>> byDay = readings.stream()
                .collect(Collectors.groupingBy(
                        reading -> reading.getTakenAt().atZone(zoneId).toLocalDate()));
        Map<LocalDate, BigDecimal> distances = new TreeMap<>();
        byDay.forEach((day, dayReadings) -> {
            dayReadings.sort(Comparator.comparing(OdometerReading::getTakenAt));
            BigDecimal first = dayReadings.getFirst().getReadingKm();
            BigDecimal last = dayReadings.getLast().getReadingKm();
            distances.put(day, last.subtract(first));
        });
        return distances;
    }
}
