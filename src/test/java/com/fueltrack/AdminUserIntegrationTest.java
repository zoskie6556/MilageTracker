package com.fueltrack;

import com.fueltrack.model.DayMoment;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.dto.AdminUserDetail;
import com.fueltrack.repository.FuelFillUpRepository;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.repository.UserRepository;
import com.fueltrack.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OdometerReadingRepository readingRepository;

    @Autowired
    private FuelFillUpRepository fillUpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        fillUpRepository.deleteAll();
        readingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        admin = saveUser("admin@example.com", Role.ROLE_ADMIN, true);
        target = saveUser("driver@example.com", Role.ROLE_USER, true);
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(target);
        vehicle.setName("Family car");
        vehicle.setPlate("ABC123");
        vehicle.setCreatedAt(Instant.now());
        vehicle = vehicleRepository.save(vehicle);

        OdometerReading first = new OdometerReading();
        first.setVehicle(vehicle);
        first.setReadingKm(new BigDecimal("10000"));
        first.setTakenAt(Instant.parse("2026-11-08T06:00:00Z"));
        first.setDayMoment(DayMoment.START_OF_DAY);
        first.setSource(ReadingSource.MANUAL);
        readingRepository.save(first);
        OdometerReading last = new OdometerReading();
        last.setVehicle(vehicle);
        last.setReadingKm(new BigDecimal("10100"));
        last.setTakenAt(Instant.parse("2026-11-08T15:00:00Z"));
        last.setDayMoment(DayMoment.END_OF_DAY);
        last.setSource(ReadingSource.AI);
        readingRepository.save(last);
    }

    @Test
    void adminCanSearchUsersAndSeeReadOnlyDetailsWithoutPasswordHashes() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .param("q", "DRIVER"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("driver@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));

        var detailResult = mockMvc.perform(get("/admin/users/{id}", target.getId())
                        .with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Family car")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI")))
                .andReturn();
        AdminUserDetail detail = (AdminUserDetail) detailResult.getModelAndView().getModel().get("userDetail");
        assertThat(detail.getClass().getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("passwordHash");
    }

    @Test
    void adminCanDisableAccountAndDisabledUserCannotLogIn() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/disable", target.getId())
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertThat(userRepository.findById(target.getId()).orElseThrow().isEnabled()).isFalse();
        mockMvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders
                        .formLogin("/login").user("email", target.getEmail()).password("password123"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers
                        .unauthenticated());

        mockMvc.perform(post("/admin/users/{id}/enable", target.getId())
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders
                        .formLogin("/login").user("email", target.getEmail()).password("password123"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers
                        .authenticated());
    }

    @Test
    void lastEnabledAdministratorCannotBeDisabled() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/disable", admin.getId())
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(userRepository.findById(admin.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void nonAdminReceivesForbiddenForAdminPage() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(target.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownPageUsesFriendlyNotFoundView() throws Exception {
        mockMvc.perform(get("/missing-page").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page not found")));
    }

    private User saveUser(String email, Role role, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(enabled);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }
}
