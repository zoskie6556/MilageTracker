package com.fueltrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "odometer_readings")
public class OdometerReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "reading_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal readingKm;

    @Column(name = "taken_at", nullable = false)
    private Instant takenAt;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReadingSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_moment", nullable = false, length = 20)
    private DayMoment dayMoment;

    @Column(name = "ai_confidence", length = 10)
    private String aiConfidence;

    @Column(length = 500)
    private String note;

    public OdometerReading() {
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public BigDecimal getReadingKm() {
        return readingKm;
    }

    public void setReadingKm(BigDecimal readingKm) {
        this.readingKm = readingKm;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Instant takenAt) {
        this.takenAt = takenAt;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public ReadingSource getSource() {
        return source;
    }

    public void setSource(ReadingSource source) {
        this.source = source;
    }

    public DayMoment getDayMoment() {
        return dayMoment;
    }

    public void setDayMoment(DayMoment dayMoment) {
        this.dayMoment = dayMoment;
    }

    public String getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(String aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
