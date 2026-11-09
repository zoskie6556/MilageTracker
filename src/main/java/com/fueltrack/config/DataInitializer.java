package com.fueltrack.config;

import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import com.fueltrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ROLE_ADMIN)) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            logger.warn("No admin account exists. Set ADMIN_EMAIL and ADMIN_PASSWORD to seed one.");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
        logger.info("Seeded the initial administrator account.");
    }
}
