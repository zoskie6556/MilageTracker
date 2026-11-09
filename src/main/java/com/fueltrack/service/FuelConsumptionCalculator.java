package com.fueltrack.service;

import com.fueltrack.dto.FuelConsumption;
import com.fueltrack.model.FuelFillUp;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FuelConsumptionCalculator {

    /**
     * Calculates fuel and cost consumption between consecutive full-tank readings.
     *
     * <p>Fuel and cost from partial fill-ups between two full tanks are included. Intervals with
     * non-positive odometer distance are omitted because they cannot produce meaningful rates.</p>
     *
     * @param fillUps fill-ups for one vehicle
     * @return valid full-tank interval consumption values in chronological order
     */
    public List<FuelConsumption> calculate(List<FuelFillUp> fillUps) {
        List<FuelFillUp> ordered = fillUps.stream()
                .sorted(Comparator.comparing(FuelFillUp::getFilledAt))
                .toList();
        List<FuelConsumption> results = new ArrayList<>();
        FuelFillUp previousFullTank = null;
        BigDecimal intervalLitres = BigDecimal.ZERO;
        BigDecimal intervalCost = BigDecimal.ZERO;

        for (FuelFillUp fillUp : ordered) {
            if (previousFullTank == null) {
                if (fillUp.isFullTank()) {
                    previousFullTank = fillUp;
                }
                continue;
            }

            intervalLitres = intervalLitres.add(fillUp.getLitres());
            intervalCost = intervalCost.add(fillUp.getTotalCost());
            if (!fillUp.isFullTank()) {
                continue;
            }

            BigDecimal distanceKm = fillUp.getOdometerKm().subtract(previousFullTank.getOdometerKm());
            if (distanceKm.signum() > 0) {
                results.add(new FuelConsumption(
                        fillUp.getVehicle().getId(),
                        fillUp.getVehicle().getName(),
                        previousFullTank.getFilledAt(),
                        fillUp.getFilledAt(),
                        distanceKm,
                        intervalLitres,
                        intervalCost,
                        intervalLitres.multiply(BigDecimal.valueOf(100))
                                .divide(distanceKm, 2, RoundingMode.HALF_UP),
                        intervalCost.divide(distanceKm, 2, RoundingMode.HALF_UP)));
            }
            previousFullTank = fillUp;
            intervalLitres = BigDecimal.ZERO;
            intervalCost = BigDecimal.ZERO;
        }
        return List.copyOf(results);
    }
}
