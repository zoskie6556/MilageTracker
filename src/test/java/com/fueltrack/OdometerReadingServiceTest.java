package com.fueltrack;

import com.fueltrack.dto.ConfirmReadingForm;
import com.fueltrack.dto.PendingReading;
import com.fueltrack.model.DayMoment;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.repository.VehicleRepository;
import com.fueltrack.service.OdometerReadingService;
import com.fueltrack.service.WarningConfirmationRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OdometerReadingServiceTest {

    @Mock
    private OdometerReadingRepository readingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private OdometerReadingService service;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        service = new OdometerReadingService(readingRepository, vehicleRepository);
        User user = new User();
        vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setName("Car");
    }

    @Test
    void savesManualReadingWithoutPhotoOrAiConfidence() {
        stubSaving();
        ConfirmReadingForm form = form(78000, false);
        PendingReading pending = pending(ReadingSource.MANUAL, null, null);

        OdometerReading saved = service.saveConfirmed(7L, pending, form);

        assertThat(saved.getSource()).isEqualTo(ReadingSource.MANUAL);
        assertThat(saved.getPhotoPath()).isNull();
        assertThat(saved.getAiConfidence()).isNull();
        assertThat(saved.getDayMoment()).isEqualTo(DayMoment.END_OF_DAY);
        assertThat(saved.getReadingKm()).isEqualByComparingTo("78000");
    }

    @Test
    void savesEditedAiValueWithAiEditedSource() {
        stubSaving();
        ConfirmReadingForm form = form(12501, false);
        PendingReading pending = pending(ReadingSource.AI, 12500, "high");

        OdometerReading saved = service.saveConfirmed(7L, pending, form);

        assertThat(saved.getSource()).isEqualTo(ReadingSource.AI_EDITED);
        assertThat(saved.getPhotoPath()).isEqualTo("photo.jpg");
        assertThat(saved.getAiConfidence()).isEqualTo("high");
    }

    @Test
    void warnsForLowerValueAndRequiresExplicitOverride() {
        stubSaving();
        OdometerReading previous = new OdometerReading();
        previous.setReadingKm(BigDecimal.valueOf(90000));
        when(readingRepository.findFirstByVehicleIdAndTakenAtBeforeOrderByTakenAtDesc(
                org.mockito.ArgumentMatchers.eq(3L), any(Instant.class)))
                .thenReturn(Optional.of(previous));
        PendingReading pending = pending(ReadingSource.MANUAL, null, null);

        assertThatThrownBy(() -> service.saveConfirmed(7L, pending, form(89000, false)))
                .isInstanceOf(WarningConfirmationRequiredException.class)
                .hasMessageContaining("confirm");

        OdometerReading saved = service.saveConfirmed(7L, pending, form(89000, true));
        assertThat(saved.getReadingKm()).isEqualByComparingTo("89000");
    }

    @Test
    void warnsOnlyWhenIncreaseExceedsOneThousandKilometres() {
        when(vehicleRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(vehicle));
        OdometerReading previous = new OdometerReading();
        previous.setReadingKm(BigDecimal.valueOf(90000));
        when(readingRepository.findFirstByVehicleIdAndTakenAtBeforeOrderByTakenAtDesc(
                org.mockito.ArgumentMatchers.eq(3L), any(Instant.class)))
                .thenReturn(Optional.of(previous));

        assertThat(service.findWarnings(7L, 3L, Instant.parse("2026-01-02T00:00:00Z"), 91000)).isEmpty();
        assertThat(service.findWarnings(7L, 3L, Instant.parse("2026-01-02T00:00:00Z"), 91001))
                .extracting("message")
                .containsExactly("This reading jumps more than 1,000 km from the previous reading.");
    }

    private static ConfirmReadingForm form(int readingKm, boolean confirmWarning) {
        ConfirmReadingForm form = new ConfirmReadingForm();
        form.setVehicleId(3L);
        form.setReadingKm(readingKm);
        form.setTakenAt(LocalDateTime.of(2026, 11, 9, 18, 30));
        form.setDayMoment(DayMoment.END_OF_DAY);
        form.setConfirmWarning(confirmWarning);
        return form;
    }

    private void stubSaving() {
        when(vehicleRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(vehicle));
        when(readingRepository.save(any(OdometerReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static PendingReading pending(ReadingSource source, Integer aiValue, String confidence) {
        return new PendingReading(7L, 3L, source == ReadingSource.MANUAL ? null : "photo.jpg",
                aiValue, aiValue == null ? 78000 : aiValue,
                LocalDateTime.of(2026, 11, 9, 18, 30), DayMoment.END_OF_DAY,
                "test note", source, confidence, null);
    }
}
