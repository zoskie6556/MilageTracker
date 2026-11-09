package com.fueltrack.repository;

import com.fueltrack.model.FuelFillUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FuelFillUpRepository extends JpaRepository<FuelFillUp, Long> {

    Optional<FuelFillUp> findByIdAndVehicleUserId(Long id, Long userId);

    @Query("select fillUp from FuelFillUp fillUp join fetch fillUp.vehicle "
            + "where fillUp.vehicle.user.id = :userId order by fillUp.filledAt desc")
    List<FuelFillUp> findAllOwnedByUserIdOrderByFilledAtDesc(@Param("userId") Long userId);

    @Query("select fillUp from FuelFillUp fillUp "
            + "where fillUp.vehicle.id = :vehicleId and fillUp.vehicle.user.id = :userId "
            + "order by fillUp.filledAt asc")
    List<FuelFillUp> findAllByVehicleIdAndUserIdOrderByFilledAt(
            @Param("vehicleId") Long vehicleId, @Param("userId") Long userId);
}
