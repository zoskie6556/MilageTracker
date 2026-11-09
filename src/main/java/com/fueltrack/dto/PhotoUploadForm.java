package com.fueltrack.dto;

import com.fueltrack.model.DayMoment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class PhotoUploadForm {

    @NotNull(message = "Choose a vehicle.")
    private Long vehicleId;

    @NotNull(message = "Choose the date and time.")
    private LocalDateTime takenAt;

    @NotNull(message = "Choose start or end of day.")
    private DayMoment dayMoment = DayMoment.START_OF_DAY;

    @Size(max = 500)
    private String note;

    private MultipartFile photo;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
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

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }
}
