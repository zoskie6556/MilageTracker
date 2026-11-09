package com.fueltrack;

import com.fueltrack.model.DayMoment;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.service.MileageSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MileageSummaryServiceTest {

    @Mock
    private OdometerReadingRepository readingRepository;

    @InjectMocks
    private MileageSummaryService summaryService;

    @Test
    void groupsMixedSourceReadingsIntoGaboroneDailyAndMonthlyTotals() {
        Vehicle vehicle = new Vehicle();
        vehicle.setName("Car");
        vehicle.setUser(new User());
        ReflectionTestUtils.setField(vehicle, "id", 5L);

        OdometerReading morning = reading(vehicle, "2026-05-01T06:00:00Z", "1000", ReadingSource.MANUAL);
        OdometerReading evening = reading(vehicle, "2026-05-01T15:00:00Z", "1080", ReadingSource.AI);
        OdometerReading nextDayStart = reading(vehicle, "2026-05-02T06:00:00Z", "1080", ReadingSource.MANUAL);
        OdometerReading nextDayEnd = reading(vehicle, "2026-05-02T15:00:00Z", "1130", ReadingSource.AI_EDITED);
        when(readingRepository.findAllOwnedByUserIdOrderByTakenAtDesc(9L))
                .thenReturn(List.of(nextDayEnd, nextDayStart, evening, morning));

        MileageSummaryService.MileageSummary summary = summaryService.summarize(9L);

        assertThat(summary.daily()).hasSize(2);
        assertThat(summary.daily()).extracting("distanceKm")
                .containsExactly(new BigDecimal("50"), new BigDecimal("80"));
        assertThat(summary.monthly()).hasSize(1);
        assertThat(summary.monthly().getFirst().distanceKm()).isEqualByComparingTo("130");
    }

    private static OdometerReading reading(Vehicle vehicle, String at, String km, ReadingSource source) {
        OdometerReading reading = new OdometerReading();
        reading.setVehicle(vehicle);
        reading.setTakenAt(Instant.parse(at));
        reading.setReadingKm(new BigDecimal(km));
        reading.setSource(source);
        reading.setDayMoment(DayMoment.START_OF_DAY);
        return reading;
    }

}
