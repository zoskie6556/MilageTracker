package com.fueltrack;

import com.fueltrack.model.Role;
import com.fueltrack.model.User;
import com.fueltrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createNormalUser() {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void normalUserIsForbiddenFromAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/users").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginUsesEmailAndAcceptsValidCredentials() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "user@example.com").password("password123"))
                .andExpect(authenticated().withUsername("user@example.com"));
    }

    @Test
    void unknownEmailGetsGenericAuthenticationFailure() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "missing@example.com").password("password123"))
                .andExpect(unauthenticated());
    }
}
