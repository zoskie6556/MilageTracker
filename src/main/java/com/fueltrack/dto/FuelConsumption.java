package com.fueltrack.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FuelConsumption(
        Long vehicleId,
        String vehicleName,
        Instant from,
        Instant to,
        BigDecimal distanceKm,
        BigDecimal litres,
        BigDecimal cost,
        BigDecimal litresPer100Km,
        BigDecimal costPerKm) {
}
