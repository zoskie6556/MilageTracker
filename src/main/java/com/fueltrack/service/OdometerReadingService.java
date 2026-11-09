package com.fueltrack.service;

import com.fueltrack.dto.ConfirmReadingForm;
import com.fueltrack.dto.PendingReading;
import com.fueltrack.model.DayMoment;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class OdometerReadingService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Africa/Gaborone");
    private final OdometerReadingRepository readingRepository;
    private final VehicleRepository vehicleRepository;

    public OdometerReadingService(
            OdometerReadingRepository readingRepository,
            VehicleRepository vehicleRepository) {
        this.readingRepository = readingRepository;
        this.vehicleRepository = vehicleRepository;
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

    /**
     * Computes warnings against the nearest earlier reading for the selected vehicle.
     *
     * @param userId authenticated owner identifier
     * @param vehicleId selected vehicle
     * @param takenAt reading timestamp
     * @param readingKm odometer value
     * @return warnings requiring an explicit user override
     */
    @Transactional(readOnly = true)
    public List<ReadingWarning> findWarnings(Long userId, Long vehicleId, Instant takenAt, Integer readingKm) {
        vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
        return warnings(vehicleId, takenAt, readingKm);
    }

    /**
     * Persists a confirmed reading while enforcing ownership and warning acknowledgement.
     *
     * @param userId authenticated owner
     * @param pending staged AI/manual information
     * @param form user-confirmed reading values
     * @return persisted odometer reading
     */
    @Transactional
    public OdometerReading saveConfirmed(Long userId, PendingReading pending, ConfirmReadingForm form) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(form.getVehicleId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
        Instant takenAt = form.getTakenAt().atZone(DISPLAY_ZONE).toInstant();
        List<ReadingWarning> warnings = warnings(form.getVehicleId(), takenAt, form.getReadingKm());
        if (!warnings.isEmpty() && !form.isConfirmWarning()) {
            throw new WarningConfirmationRequiredException(warnings);
        }

        ReadingSource source = pending.source();
        if (source == ReadingSource.AI && !form.getReadingKm().equals(pending.aiDetectedKm())) {
            source = ReadingSource.AI_EDITED;
        }

        OdometerReading reading = new OdometerReading();
        reading.setVehicle(vehicle);
        reading.setReadingKm(BigDecimal.valueOf(form.getReadingKm()));
        reading.setTakenAt(takenAt);
        reading.setDayMoment(form.getDayMoment());
        reading.setPhotoPath(pending.photoPath());
        reading.setSource(source);
        reading.setAiConfidence(source == ReadingSource.MANUAL ? null : pending.aiConfidence());
        reading.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());
        return readingRepository.save(reading);
    }

    /**
     * Returns the authenticated user's readings with vehicles fetched for display.
     *
     * @param userId authenticated owner
     * @return readings newest first
     */
    @Transactional(readOnly = true)
    public List<OdometerReading> findHistory(Long userId) {
        return readingRepository.findAllOwnedByUserIdOrderByTakenAtDesc(userId);
    }

    /**
     * Checks whether a stored photo is attached to one of the user's readings.
     *
     * @param filename photo filename
     * @param userId authenticated owner
     * @return true only for an owned photo
     */
    @Transactional(readOnly = true)
    public boolean ownsPhoto(String filename, Long userId) {
        return readingRepository.existsByPhotoPathAndVehicleUserId(filename, userId);
    }

    private List<ReadingWarning> warnings(Long vehicleId, Instant takenAt, Integer readingKm) {
        List<ReadingWarning> warnings = new ArrayList<>();
        readingRepository.findFirstByVehicleIdAndTakenAtBeforeOrderByTakenAtDesc(vehicleId, takenAt)
                .ifPresent(previous -> {
                    BigDecimal current = BigDecimal.valueOf(readingKm);
                    BigDecimal difference = current.subtract(previous.getReadingKm());
                    if (difference.signum() < 0) {
                        warnings.add(new ReadingWarning("This reading is lower than the previous reading for this vehicle."));
                    } else if (difference.compareTo(BigDecimal.valueOf(1000)) > 0) {
                        warnings.add(new ReadingWarning("This reading jumps more than 1,000 km from the previous reading."));
                    }
                });
        return List.copyOf(warnings);
    }
}
