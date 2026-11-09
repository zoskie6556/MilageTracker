package com.fueltrack;

import com.fueltrack.dto.RegistrationForm;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationFormTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidEmailAndPassword() {
        RegistrationForm form = new RegistrationForm();
        form.setEmail("driver@example.com");
        form.setPassword("secure123");
        form.setConfirmPassword("secure123");

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsMalformedEmailAndPasswordWithoutLetterOrDigit() {
        RegistrationForm form = new RegistrationForm();
        form.setEmail("not-an-email");
        form.setPassword("allletters");
        form.setConfirmPassword("allletters");

        assertThat(validator.validate(form)).hasSize(2);
    }
}
