package com.fueltrack;

import com.fueltrack.dto.FuelFillUpForm;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FuelFillUpFormValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidFillUpValues() {
        FuelFillUpForm form = validForm();

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsNegativeAmountsAndValuesExceedingDatabasePrecision() {
        FuelFillUpForm form = validForm();
        form.setLitres(new BigDecimal("-1"));
        form.setTotalCost(new BigDecimal("10000000000"));
        form.setOdometerKm(new BigDecimal("12345678901"));

        assertThat(validator.validate(form)).hasSize(3);
    }

    private static FuelFillUpForm validForm() {
        FuelFillUpForm form = new FuelFillUpForm();
        form.setVehicleId(1L);
        form.setFilledAt(LocalDateTime.of(2026, 11, 9, 12, 0));
        form.setLitres(new BigDecimal("35.500"));
        form.setTotalCost(new BigDecimal("52.75"));
        form.setOdometerKm(new BigDecimal("90500"));
        return form;
    }
}
