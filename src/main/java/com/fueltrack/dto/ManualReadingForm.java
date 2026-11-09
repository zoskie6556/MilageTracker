package com.fueltrack.dto;

import com.fueltrack.model.DayMoment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ManualReadingForm {

    @NotNull(message = "Choose a vehicle.")
    private Long vehicleId;

    @NotNull(message = "Enter the odometer reading.")
    @Positive(message = "Mileage must be a positive whole number.")
    private Integer readingKm;

    @NotNull(message = "Choose the date and time.")
    private LocalDateTime takenAt;

    @NotNull(message = "Choose start or end of day.")
    private DayMoment dayMoment = DayMoment.START_OF_DAY;

    @Size(max = 500)
    private String note;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getReadingKm() {
        return readingKm;
    }

    public void setReadingKm(Integer readingKm) {
        this.readingKm = readingKm;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public DayMoment getDayMoment() {
        return dayMoment;
    }

    public void setDayMoment(DayMoment dayMoment) {
        this.dayMoment = dayMoment;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
