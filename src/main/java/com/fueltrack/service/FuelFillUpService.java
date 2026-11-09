package com.fueltrack.service;

import com.fueltrack.dto.FuelFillUpForm;
import com.fueltrack.model.FuelFillUp;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.FuelFillUpRepository;
import com.fueltrack.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class FuelFillUpService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Africa/Gaborone");

    private final FuelFillUpRepository fillUpRepository;
    private final VehicleRepository vehicleRepository;

    public FuelFillUpService(FuelFillUpRepository fillUpRepository, VehicleRepository vehicleRepository) {
        this.fillUpRepository = fillUpRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Lists only fill-ups belonging to the authenticated user.
     *
     * @param userId owner identifier
     * @return fill-ups newest first
     */
    @Transactional(readOnly = true)
    public List<FuelFillUp> findHistory(Long userId) {
        return fillUpRepository.findAllOwnedByUserIdOrderByFilledAtDesc(userId);
    }

    /**
     * Lists fill-ups for one vehicle after verifying that it belongs to the user.
     *
     * @param vehicleId selected vehicle
     * @param userId owner identifier
     * @return fill-ups oldest first
     */
    @Transactional(readOnly = true)
    public List<FuelFillUp> findVehicleHistory(Long vehicleId, Long userId) {
        requireOwnedVehicle(vehicleId, userId);
        return fillUpRepository.findAllByVehicleIdAndUserIdOrderByFilledAt(vehicleId, userId);
    }

    /**
     * Returns an existing fill-up only when it belongs to the authenticated user.
     *
     * @param fillUpId fill-up identifier
     * @param userId owner identifier
     * @return owned fill-up
     */
    @Transactional(readOnly = true)
    public FuelFillUp getOwned(Long fillUpId, Long userId) {
        return fillUpRepository.findByIdAndVehicleUserId(fillUpId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Fuel fill-up not found"));
    }

    /**
     * Creates a fill-up attached to one of the authenticated user's vehicles.
     *
     * @param userId owner identifier
     * @param form validated fill-up form
     * @return persisted fill-up
     */
    @Transactional
    public FuelFillUp create(Long userId, FuelFillUpForm form) {
        FuelFillUp fillUp = new FuelFillUp();
        apply(fillUp, userId, form);
        return fillUpRepository.save(fillUp);
    }

    /**
     * Updates an owned fill-up and validates the replacement vehicle's ownership.
     *
     * @param fillUpId fill-up identifier
     * @param userId owner identifier
     * @param form validated fill-up form
     * @return updated fill-up
     */
    @Transactional
    public FuelFillUp update(Long fillUpId, Long userId, FuelFillUpForm form) {
        FuelFillUp fillUp = getOwned(fillUpId, userId);
        apply(fillUp, userId, form);
        return fillUpRepository.save(fillUp);
    }

    /**
     * Deletes an owned fill-up.
     *
     * @param fillUpId fill-up identifier
     * @param userId owner identifier
     */
    @Transactional
    public void delete(Long fillUpId, Long userId) {
        fillUpRepository.delete(getOwned(fillUpId, userId));
    }

    private void apply(FuelFillUp fillUp, Long userId, FuelFillUpForm form) {
        Vehicle vehicle = requireOwnedVehicle(form.getVehicleId(), userId);
        fillUp.setVehicle(vehicle);
        fillUp.setFilledAt(form.getFilledAt().atZone(DISPLAY_ZONE).toInstant());
        fillUp.setLitres(form.getLitres());
        fillUp.setTotalCost(form.getTotalCost());
        fillUp.setOdometerKm(form.getOdometerKm());
        fillUp.setFullTank(form.isFullTank());
    }

    private Vehicle requireOwnedVehicle(Long vehicleId, Long userId) {
        return vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
    }

    /**
     * Converts a stored instant to the local date-time used by fill-up forms.
     *
     * @param fillUp fill-up to display
     * @return local Gaborone date-time
     */
    public LocalDateTime toLocalDateTime(FuelFillUp fillUp) {
        return fillUp.getFilledAt().atZone(DISPLAY_ZONE).toLocalDateTime();
    }
}
