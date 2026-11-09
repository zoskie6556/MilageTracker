package com.fueltrack.service;

import com.fueltrack.model.User;
import com.fueltrack.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the authenticated account by its login email.
     *
     * @param email authenticated principal's email
     * @return the corresponding user
     */
    @Transactional(readOnly = true)
    public User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
