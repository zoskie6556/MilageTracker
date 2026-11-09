package com.fueltrack.dto;

import com.fueltrack.model.DayMoment;
import com.fueltrack.model.ReadingSource;

import java.time.LocalDateTime;

public record PendingReading(
        Long ownerId,
        Long vehicleId,
        String photoPath,
        Integer aiDetectedKm,
        Integer initialReadingKm,
        LocalDateTime takenAt,
        DayMoment dayMoment,
        String note,
        ReadingSource source,
        String aiConfidence,
        String aiNotes) {
}
