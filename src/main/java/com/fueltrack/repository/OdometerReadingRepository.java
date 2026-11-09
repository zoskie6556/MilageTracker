package com.fueltrack.repository;

import com.fueltrack.model.OdometerReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OdometerReadingRepository extends JpaRepository<OdometerReading, Long> {

    Optional<OdometerReading> findByIdAndVehicleUserId(Long id, Long userId);
}
