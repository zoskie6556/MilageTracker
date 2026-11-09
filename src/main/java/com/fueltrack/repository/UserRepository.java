package com.fueltrack.repository;

import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    long countByRoleAndEnabledTrue(Role role);
}
