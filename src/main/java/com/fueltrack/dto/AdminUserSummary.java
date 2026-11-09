package com.fueltrack.dto;

import com.fueltrack.model.Role;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminUserSummary(
        Long id,
        String email,
        Role role,
        boolean enabled,
        long vehicleCount,
        BigDecimal totalKmRecorded,
        Instant lastReadingAt) {
}
