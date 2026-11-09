package com.fueltrack;

import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.DayMoment;
import com.fueltrack.model.FuelFillUp;
import com.fueltrack.model.Role;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.OdometerReadingRepository;
import com.fueltrack.repository.FuelFillUpRepository;
import com.fueltrack.repository.UserRepository;
import com.fueltrack.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "ANTHROPIC_API_KEY=",
        "app.upload-dir=./target/test-uploads"
})
class ReadingWorkflowIntegrationTest {

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

    private User user;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        fillUpRepository.deleteAll();
        readingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("reading@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);

        vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setName("Daily car");
        vehicle.setCreatedAt(Instant.now());
        vehicle = vehicleRepository.save(vehicle);
    }

    @Test
    void manualEntryUsesSharedConfirmationAndSavesWithoutPhoto() throws Exception {
        MvcResult start = mockMvc.perform(post("/readings/new/manual")
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("vehicleId", vehicle.getId().toString())
                        .param("readingKm", "84510")
                        .param("takenAt", "2026-11-09T08:00")
                        .param("dayMoment", "START_OF_DAY")
                        .param("note", "Start of day"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings/confirm"))
                .andReturn();

        assertThat(readingRepository.count()).isZero();
        MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
        mockMvc.perform(get("/readings/confirm")
                        .with(user(user.getEmail()).roles("USER"))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("readings/confirm"));

        mockMvc.perform(post("/readings/confirm")
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .session(session)
                        .param("vehicleId", vehicle.getId().toString())
                        .param("readingKm", "84510")
                        .param("takenAt", "2026-11-09T08:00")
                        .param("dayMoment", "START_OF_DAY")
                        .param("note", "Start of day"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings"));

        OdometerReading saved = readingRepository.findAll().getFirst();
        assertThat(saved.getPhotoPath()).isNull();
        assertThat(saved.getSource().name()).isEqualTo("MANUAL");
        assertThat(saved.getAiConfidence()).isNull();
        mockMvc.perform(get("/readings").with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("readings/history"));
    }

    @Test
    void aiFailureRedirectsToManualEntryWithThePhotoStillAttached() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        MockMultipartFile photo = new MockMultipartFile("photo", "dash.png", "image/png", bytes.toByteArray());

        MvcResult result = mockMvc.perform(multipart("/readings/photo")
                        .file(photo)
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("vehicleId", vehicle.getId().toString())
                        .param("takenAt", "2026-11-09T08:00")
                        .param("dayMoment", "START_OF_DAY")
                        .param("note", "Before departure"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings/new/manual?aiFailure"))
                .andReturn();

        assertThat(readingRepository.count()).isZero();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/readings/new/manual")
                        .with(user(user.getEmail()).roles("USER"))
                        .session(session)
                        .param("aiFailure", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("readings/manual"));

        MvcResult confirmation = mockMvc.perform(post("/readings/new/manual")
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .session(session)
                        .param("vehicleId", vehicle.getId().toString())
                        .param("readingKm", "84510")
                        .param("takenAt", "2026-11-09T08:00")
                        .param("dayMoment", "START_OF_DAY")
                        .param("note", "Before departure"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings/confirm"))
                .andReturn();

        MockHttpSession confirmSession = (MockHttpSession) confirmation.getRequest().getSession(false);
        mockMvc.perform(post("/readings/confirm")
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .session(confirmSession)
                        .param("vehicleId", vehicle.getId().toString())
                        .param("readingKm", "84510")
                        .param("takenAt", "2026-11-09T08:00")
                        .param("dayMoment", "START_OF_DAY")
                        .param("note", "Before departure"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings"));

        OdometerReading saved = readingRepository.findAll().getFirst();
        assertThat(saved.getPhotoPath()).isNotBlank();
        assertThat(saved.getSource().name()).isEqualTo("MANUAL");
        assertThat(saved.getAiConfidence()).isNull();
        mockMvc.perform(get("/readings").with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("readings/history"));
    }

    @Test
    void readingHistoryRendersOnlyForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/readings")
                        .with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("readings/history"));
    }

    @Test
    void ownerCanEditAndDeleteAReading() throws Exception {
        OdometerReading reading = new OdometerReading();
        reading.setVehicle(vehicle);
        reading.setReadingKm(new java.math.BigDecimal("90000"));
        reading.setTakenAt(Instant.parse("2026-11-09T06:00:00Z"));
        reading.setDayMoment(DayMoment.START_OF_DAY);
        reading.setSource(ReadingSource.AI);
        reading = readingRepository.save(reading);

        mockMvc.perform(get("/readings/{id}/edit", reading.getId())
                        .with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("readings/edit"));

        mockMvc.perform(post("/readings/{id}/edit", reading.getId())
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("vehicleId", vehicle.getId().toString())
                        .param("readingKm", "90001")
                        .param("takenAt", "2026-11-09T08:00")
                        .param("dayMoment", "END_OF_DAY")
                        .param("note", "Corrected"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings"));

        OdometerReading updated = readingRepository.findById(reading.getId()).orElseThrow();
        assertThat(updated.getSource()).isEqualTo(ReadingSource.AI_EDITED);
        assertThat(updated.getReadingKm()).isEqualByComparingTo("90001");

        mockMvc.perform(post("/readings/{id}/delete", reading.getId())
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/readings"));
        assertThat(readingRepository.existsById(reading.getId())).isFalse();
    }

    @Test
    void ownerCanCreateEditAndDeleteAFuelFillUp() throws Exception {
        mockMvc.perform(get("/fill-ups").with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("fill-ups"));

        MvcResult create = mockMvc.perform(post("/fill-ups")
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("vehicleId", vehicle.getId().toString())
                        .param("filledAt", "2026-11-09T08:00")
                        .param("litres", "35.5")
                        .param("totalCost", "52.75")
                        .param("odometerKm", "90500")
                        .param("fullTank", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fill-ups"))
                .andReturn();

        FuelFillUp fillUp = fillUpRepository.findAll().getFirst();
        MockHttpSession session = (MockHttpSession) create.getRequest().getSession(false);
        mockMvc.perform(get("/fill-ups/{id}/edit", fillUp.getId())
                        .with(user(user.getEmail()).roles("USER"))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("fill-ups"));

        mockMvc.perform(post("/fill-ups/{id}/edit", fillUp.getId())
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("vehicleId", vehicle.getId().toString())
                        .param("filledAt", "2026-11-09T09:00")
                        .param("litres", "36")
                        .param("totalCost", "54")
                        .param("odometerKm", "90510")
                        .param("fullTank", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fill-ups"));

        assertThat(fillUpRepository.findById(fillUp.getId()).orElseThrow().isFullTank()).isFalse();
        mockMvc.perform(post("/fill-ups/{id}/delete", fillUp.getId())
                        .with(user(user.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fill-ups"));
        assertThat(fillUpRepository.existsById(fillUp.getId())).isFalse();
    }

    @Test
    void ownerCanViewDailyAndMonthlyMileageSummary() throws Exception {
        OdometerReading first = new OdometerReading();
        first.setVehicle(vehicle);
        first.setReadingKm(new java.math.BigDecimal("1000"));
        first.setTakenAt(Instant.parse("2026-11-09T06:00:00Z"));
        first.setDayMoment(DayMoment.START_OF_DAY);
        first.setSource(ReadingSource.MANUAL);
        readingRepository.save(first);

        OdometerReading last = new OdometerReading();
        last.setVehicle(vehicle);
        last.setReadingKm(new java.math.BigDecimal("1080"));
        last.setTakenAt(Instant.parse("2026-11-09T15:00:00Z"));
        last.setDayMoment(DayMoment.END_OF_DAY);
        last.setSource(ReadingSource.AI);
        readingRepository.save(last);

        mockMvc.perform(get("/summary").with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("summary"));
    }
}
