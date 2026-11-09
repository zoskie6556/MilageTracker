package com.fueltrack.service;

import com.fueltrack.model.OdometerReading;
import com.fueltrack.repository.OdometerReadingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OdometerReadingService {

    private final OdometerReadingRepository readingRepository;

    public OdometerReadingService(OdometerReadingRepository readingRepository) {
        this.readingRepository = readingRepository;
    }

    /**
     * Finds an odometer reading only when it belongs to the requesting user.
     *
     * @param readingId reading identifier
     * @param userId authenticated user's identifier
     * @return the owned reading
     * @throws EntityNotFoundException if the reading does not exist or is not owned by the user
     */
    @Transactional(readOnly = true)
    public OdometerReading getOwnedReading(Long readingId, Long userId) {
        return readingRepository.findByIdAndVehicleUserId(readingId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Reading not found"));
    }
}
