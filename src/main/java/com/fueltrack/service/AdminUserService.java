package com.fueltrack.service;

import com.fueltrack.dto.AdminUserDetail;
import com.fueltrack.dto.AdminUserSummary;
import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import com.fueltrack.repository.FuelFillUpRepository;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.repository.UserRepository;
import com.fueltrack.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final OdometerReadingRepository readingRepository;
    private final FuelFillUpRepository fillUpRepository;
    private final MileageSummaryService mileageSummaryService;

    public AdminUserService(
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            OdometerReadingRepository readingRepository,
            FuelFillUpRepository fillUpRepository,
            MileageSummaryService mileageSummaryService) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.readingRepository = readingRepository;
        this.fillUpRepository = fillUpRepository;
        this.mileageSummaryService = mileageSummaryService;
    }

    /**
     * Searches users by email and returns summary data without exposing credentials.
     *
     * @param emailQuery optional email substring
     * @param page zero-based page index
     * @param pageSize page size, limited to 100
     * @return paginated user summaries
     */
    @Transactional(readOnly = true)
    public Page<AdminUserSummary> search(String emailQuery, int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Invalid pagination values.");
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("email").ascending());
        Page<User> users = emailQuery == null || emailQuery.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCase(emailQuery.trim(), pageable);
        return users.map(this::toSummary);
    }

    /**
     * Retrieves the read-only vehicle, reading and fill-up data for one user.
     *
     * @param userId target user identifier
     * @return detail data without the password hash
     */
    @Transactional(readOnly = true)
    public AdminUserDetail getDetail(Long userId) {
        User user = requireUser(userId);
        return AdminUserDetail.from(
                user,
                vehicleRepository.findAllByUserIdOrderByName(userId),
                readingRepository.findAllOwnedByUserIdOrderByTakenAtDesc(userId),
                fillUpRepository.findAllOwnedByUserIdOrderByFilledAtDesc(userId));
    }

    /**
     * Enables or disables a target account while preventing administrator lockout.
     *
     * @param actorId administrator making the change
     * @param targetId account to update
     * @param enabled requested account status
     */
    @Transactional
    public void setEnabled(Long actorId, Long targetId, boolean enabled) {
        User target = requireUser(targetId);
        if (!enabled && target.isEnabled()) {
            if (actorId.equals(targetId)) {
                throw new IllegalStateException("You cannot disable your own administrator account.");
            }
            if (target.getRole() == Role.ROLE_ADMIN
                    && userRepository.countByRoleAndEnabledTrue(Role.ROLE_ADMIN) <= 1) {
                throw new IllegalStateException("The last enabled administrator cannot be disabled.");
            }
        }
        target.setEnabled(enabled);
        userRepository.save(target);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private AdminUserSummary toSummary(User user) {
        MileageSummaryService.MileageSummary summary = mileageSummaryService.summarize(user.getId());
        BigDecimal totalDistance = summary.daily().stream()
                .map(item -> item.distanceKm())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var readings = readingRepository.findAllOwnedByUserIdOrderByTakenAtDesc(user.getId());
        return new AdminUserSummary(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                vehicleRepository.countByUserId(user.getId()),
                totalDistance,
                readings.isEmpty() ? null : readings.getFirst().getTakenAt());
    }
}
