package com.fueltrack.service;

import com.fueltrack.dto.VehicleForm;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicleRepository, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.clock = clock;
    }

    /**
     * Lists only vehicles owned by the given user.
     *
     * @param userId owner identifier
     * @return the user's vehicles ordered by name
     */
    @Transactional(readOnly = true)
    public List<Vehicle> findVehicles(Long userId) {
        return vehicleRepository.findAllByUserIdOrderByName(userId);
    }

    /**
     * Creates a vehicle for the authenticated owner.
     *
     * @param owner user who owns the vehicle
     * @param form validated vehicle details
     * @return persisted vehicle
     */
    @Transactional
    public Vehicle createVehicle(User owner, VehicleForm form) {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(owner);
        vehicle.setName(form.getName().trim());
        vehicle.setPlate(form.getPlate() == null || form.getPlate().isBlank() ? null : form.getPlate().trim());
        vehicle.setCreatedAt(Instant.now(clock));
        return vehicleRepository.save(vehicle);
    }

    /**
     * Resolves a vehicle only when it belongs to the given user.
     *
     * @param vehicleId requested vehicle identifier
     * @param userId authenticated owner identifier
     * @return owned vehicle
     * @throws EntityNotFoundException if the vehicle is absent or not owned by this user
     */
    @Transactional(readOnly = true)
    public Vehicle requireOwnedVehicle(Long vehicleId, Long userId) {
        return vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
    }
}
