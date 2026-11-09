package com.fueltrack.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyMileageSummary(
        Long vehicleId,
        String vehicleName,
        YearMonth month,
        BigDecimal distanceKm) {
}
