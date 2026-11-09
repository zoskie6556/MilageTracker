package com.fueltrack.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FuelFillUpForm {

    @NotNull(message = "Choose a vehicle.")
    private Long vehicleId;

    @NotNull(message = "Choose the fill-up date and time.")
    private LocalDateTime filledAt;

    @NotNull(message = "Enter the number of litres.")
    @DecimalMin(value = "0.001", message = "Litres must be greater than zero.")
    @Digits(integer = 5, fraction = 3, message = "Litres must fit within 99,999.999.")
    private BigDecimal litres;

    @NotNull(message = "Enter the total cost.")
    @DecimalMin(value = "0.00", message = "Cost cannot be negative.")
    @Digits(integer = 10, fraction = 2, message = "Cost must fit within 9,999,999,999.99.")
    private BigDecimal totalCost;

    @NotNull(message = "Enter the odometer reading.")
    @DecimalMin(value = "0.01", message = "Odometer kilometres must be greater than zero.")
    @Digits(integer = 10, fraction = 2, message = "Odometer value is too large.")
    private BigDecimal odometerKm;

    private boolean fullTank;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDateTime getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(LocalDateTime filledAt) {
        this.filledAt = filledAt;
    }

    public BigDecimal getLitres() {
        return litres;
    }

    public void setLitres(BigDecimal litres) {
        this.litres = litres;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getOdometerKm() {
        return odometerKm;
    }

    public void setOdometerKm(BigDecimal odometerKm) {
        this.odometerKm = odometerKm;
    }

    public boolean isFullTank() {
        return fullTank;
    }

    public void setFullTank(boolean fullTank) {
        this.fullTank = fullTank;
    }
}
