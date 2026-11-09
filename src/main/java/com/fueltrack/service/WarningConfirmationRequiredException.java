package com.fueltrack.service;

import java.util.List;

public class WarningConfirmationRequiredException extends RuntimeException {

    private final List<ReadingWarning> warnings;

    public WarningConfirmationRequiredException(List<ReadingWarning> warnings) {
        super("Please confirm the odometer warning before saving.");
        this.warnings = List.copyOf(warnings);
    }

    public List<ReadingWarning> getWarnings() {
        return warnings;
    }
}
