package com.fueltrack.dto;

import com.fueltrack.model.DayMoment;

import java.time.LocalDateTime;

public record PendingPhoto(
        Long ownerId,
        Long vehicleId,
        String photoPath,
        LocalDateTime takenAt,
        DayMoment dayMoment,
        String note,
        String failureMessage) {
}
