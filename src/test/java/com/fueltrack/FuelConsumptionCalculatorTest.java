package com.fueltrack;

import com.fueltrack.dto.FuelConsumption;
import com.fueltrack.model.FuelFillUp;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.service.FuelConsumptionCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FuelConsumptionCalculatorTest {

    private final FuelConsumptionCalculator calculator = new FuelConsumptionCalculator();

    @Test
    void includesPartialFillUpsBetweenConsecutiveFullTanks() {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(new User());
        vehicle.setName("Car");
        FuelFillUp start = fillUp(vehicle, "2026-01-01T10:00:00Z", "40", "60", "10000", true);
        FuelFillUp partial = fillUp(vehicle, "2026-01-05T10:00:00Z", "10", "15", "10200", false);
        FuelFillUp end = fillUp(vehicle, "2026-01-10T10:00:00Z", "20", "30", "10500", true);

        List<FuelConsumption> results = calculator.calculate(List.of(end, start, partial));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().distanceKm()).isEqualByComparingTo("500");
        assertThat(results.getFirst().litres()).isEqualByComparingTo("30");
        assertThat(results.getFirst().cost()).isEqualByComparingTo("45");
        assertThat(results.getFirst().litresPer100Km()).isEqualByComparingTo("6.00");
        assertThat(results.getFirst().costPerKm()).isEqualByComparingTo("0.09");
    }

    @Test
    void ignoresIntervalsWithNonPositiveOdometerDistance() {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(new User());
        vehicle.setName("Car");

        assertThat(calculator.calculate(List.of(
                fillUp(vehicle, "2026-01-01T10:00:00Z", "40", "60", "10000", true),
                fillUp(vehicle, "2026-01-10T10:00:00Z", "40", "60", "10000", true))))
                .isEmpty();
    }

    private static FuelFillUp fillUp(
            Vehicle vehicle, String filledAt, String litres, String cost, String odometerKm, boolean fullTank) {
        FuelFillUp fillUp = new FuelFillUp();
        fillUp.setVehicle(vehicle);
        fillUp.setFilledAt(Instant.parse(filledAt));
        fillUp.setLitres(new BigDecimal(litres));
        fillUp.setTotalCost(new BigDecimal(cost));
        fillUp.setOdometerKm(new BigDecimal(odometerKm));
        fillUp.setFullTank(fullTank);
        return fillUp;
    }
}
