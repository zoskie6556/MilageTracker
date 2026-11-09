package com.fueltrack.repository;

import com.fueltrack.model.OdometerReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OdometerReadingRepository extends JpaRepository<OdometerReading, Long> {

    Optional<OdometerReading> findByIdAndVehicleUserId(Long id, Long userId);

    Optional<OdometerReading> findFirstByVehicleIdAndTakenAtBeforeOrderByTakenAtDesc(
            Long vehicleId, Instant takenAt);

    @Query("select reading from OdometerReading reading join fetch reading.vehicle "
            + "where reading.vehicle.user.id = :userId order by reading.takenAt desc")
    List<OdometerReading> findAllOwnedByUserIdOrderByTakenAtDesc(@Param("userId") Long userId);

    boolean existsByPhotoPathAndVehicleUserId(String photoPath, Long userId);
}
