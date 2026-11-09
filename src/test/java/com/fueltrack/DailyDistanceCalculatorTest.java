package com.fueltrack;

import com.fueltrack.model.DayMoment;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.service.DailyDistanceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyDistanceCalculatorTest {

    private final DailyDistanceCalculator calculator = new DailyDistanceCalculator();

    @Test
    void calculatesDistanceAcrossMixedManualAndAiSources() {
        OdometerReading start = reading("2026-11-09T06:00:00Z", "50120", ReadingSource.MANUAL);
        OdometerReading end = reading("2026-11-09T16:00:00Z", "50275", ReadingSource.AI);

        assertThat(calculator.calculate(List.of(end, start), ZoneId.of("Africa/Gaborone")))
                .containsEntry(LocalDate.of(2026, 11, 9), BigDecimal.valueOf(155));
    }

    private static OdometerReading reading(String takenAt, String km, ReadingSource source) {
        OdometerReading reading = new OdometerReading();
        reading.setTakenAt(Instant.parse(takenAt));
        reading.setReadingKm(new BigDecimal(km));
        reading.setSource(source);
        reading.setDayMoment(source == ReadingSource.MANUAL ? DayMoment.START_OF_DAY : DayMoment.END_OF_DAY);
        return reading;
    }
}
