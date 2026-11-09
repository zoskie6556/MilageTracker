package com.fueltrack;

import com.fueltrack.model.OdometerReading;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.service.OdometerReadingService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OdometerOwnershipTest {

    @Mock
    private OdometerReadingRepository readingRepository;

    @InjectMocks
    private OdometerReadingService readingService;

    @Test
    void userCannotAccessAnotherUsersReading() {
        when(readingRepository.findByIdAndVehicleUserId(42L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readingService.getOwnedReading(42L, 7L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Reading not found");
        verify(readingRepository).findByIdAndVehicleUserId(42L, 7L);
    }

    @Test
    void userCannotDeleteAnotherUsersReading() {
        when(readingRepository.findByIdAndVehicleUserId(42L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readingService.deleteOwned(42L, 7L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Reading not found");
        verify(readingRepository).findByIdAndVehicleUserId(42L, 7L);
        verify(readingRepository, never()).delete(any(OdometerReading.class));
    }
}
