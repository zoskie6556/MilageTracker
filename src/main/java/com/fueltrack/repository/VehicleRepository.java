package com.fueltrack.repository;

import com.fueltrack.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByUserIdOrderByName(Long userId);

    Optional<Vehicle> findByIdAndUserId(Long id, Long userId);
}
