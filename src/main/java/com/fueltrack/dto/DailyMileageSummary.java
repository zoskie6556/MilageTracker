package com.fueltrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMileageSummary(
        Long vehicleId,
        String vehicleName,
        LocalDate day,
        BigDecimal firstReadingKm,
        BigDecimal lastReadingKm,
        BigDecimal distanceKm) {
}
