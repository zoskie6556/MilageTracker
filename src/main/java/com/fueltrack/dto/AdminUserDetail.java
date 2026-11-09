package com.fueltrack.dto;

import com.fueltrack.model.FuelFillUp;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminUserDetail(
        Long id,
        String email,
        boolean enabled,
        List<AdminVehicleView> vehicles,
        List<AdminReadingView> readings,
        List<AdminFillUpView> fillUps) {

    public static AdminUserDetail from(
            User user, List<Vehicle> vehicles, List<OdometerReading> readings, List<FuelFillUp> fillUps) {
        return new AdminUserDetail(
                user.getId(), user.getEmail(), user.isEnabled(),
                vehicles.stream().map(vehicle -> new AdminVehicleView(
                        vehicle.getId(), vehicle.getName(), vehicle.getPlate(), vehicle.getCreatedAt())).toList(),
                readings.stream().map(reading -> new AdminReadingView(
                        reading.getId(), reading.getVehicle().getName(), reading.getReadingKm(),
                        reading.getTakenAt(), reading.getSource(), reading.getDayMoment(),
                        reading.getPhotoPath(), reading.getNote())).toList(),
                fillUps.stream().map(fillUp -> new AdminFillUpView(
                        fillUp.getId(), fillUp.getVehicle().getName(), fillUp.getFilledAt(),
                        fillUp.getLitres(), fillUp.getTotalCost(), fillUp.getOdometerKm(),
                        fillUp.isFullTank())).toList());
    }

    public record AdminVehicleView(Long id, String name, String plate, Instant createdAt) {
    }

    public record AdminReadingView(
            Long id,
            String vehicleName,
            BigDecimal readingKm,
            Instant takenAt,
            com.fueltrack.model.ReadingSource source,
            com.fueltrack.model.DayMoment dayMoment,
            String photoPath,
            String note) {
    }

    public record AdminFillUpView(
            Long id,
            String vehicleName,
            Instant filledAt,
            BigDecimal litres,
            BigDecimal totalCost,
            BigDecimal odometerKm,
            boolean fullTank) {
    }
}
