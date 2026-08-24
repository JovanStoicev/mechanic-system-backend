package com.garage.system.security;

import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired EntityManager entityManager;
    @Autowired JwtService jwtService;

    @Test
    void unauthenticatedRequestsCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/boss/mechanics")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me/jobs")).andExpect(status().isForbidden());
    }

    @Test
    void bossAndMechanicCannotAccessEachOthersEndpoints() throws Exception {
        User boss = userRepository.findByEmailIgnoreCase("boss@garage.com").orElseThrow();
        User mechanicUser = createMechanicUser(true);

        mockMvc.perform(get("/api/me/jobs")
                        .header("Authorization", bearer(jwtService.createToken(boss))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/boss/mechanics")
                        .header("Authorization", bearer(jwtService.createToken(mechanicUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveMechanicTokenCannotAccessMechanicEndpoints() throws Exception {
        User inactiveMechanic = createMechanicUser(false);

        mockMvc.perform(get("/api/me/jobs")
                        .header("Authorization", bearer(jwtService.createToken(inactiveMechanic))))
                .andExpect(status().isForbidden());
    }

    private User createMechanicUser(boolean active) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = new User();
        user.setEmail("security-" + suffix + "@test.local");
        user.setName("Security Test");
        user.setRole(UserRole.ROLE_MECHANIC);
        user.setPasswordHash("not-used");
        userRepository.save(user);

        Mechanic mechanic = new Mechanic();
        mechanic.setUser(user);
        mechanic.setFixedSalary(new BigDecimal("1000.00"));
        mechanic.setActive(active);
        if (!active) mechanic.setDeactivatedAt(OffsetDateTime.now());
        mechanicRepository.save(mechanic);
        entityManager.flush();
        return user;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
