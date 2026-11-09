package com.fueltrack.service;

import com.fueltrack.dto.RegistrationForm;
import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import com.fueltrack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates an enabled user account with the default user role.
     *
     * @param form validated registration details
     * @return the newly persisted user
     * @throws DuplicateEmailException if an account already uses the email address
     * @throws PasswordMismatchException if the password confirmation differs
     */
    @Transactional
    public User register(RegistrationForm form) {
        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    public static class DuplicateEmailException extends RuntimeException {
    }

    public static class PasswordMismatchException extends RuntimeException {
    }
}
