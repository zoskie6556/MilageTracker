package com.fueltrack;

import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import com.fueltrack.model.Vehicle;
import com.fueltrack.repository.OdometerReadingRepository;
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
    private PasswordEncoder passwordEncoder;

    private User user;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
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
}
