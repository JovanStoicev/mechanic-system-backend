package com.garage.system.part;

import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.security.JwtService;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PartRequestLifecycleIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired PartRepository partRepository;
    @Autowired PartRequestRepository requestRepository;
    @Autowired EntityManager entityManager;
    @Autowired JwtService jwtService;

    @Test
    void approvedAndReceivedRequestIncreasesStockExactlyOnce() throws Exception {
        Fixture fixture = createFixture(7);
        Long requestId = createRequest(fixture, 5);

        mockMvc.perform(patch("/api/boss/part-requests/{id}/approve", requestId)
                        .header("Authorization", bearer(fixture.bossToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        entityManager.flush();
        entityManager.clear();
        assertEquals(7, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());

        mockMvc.perform(patch("/api/boss/part-requests/{id}/receive", requestId)
                        .header("Authorization", bearer(fixture.bossToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        entityManager.flush();
        entityManager.clear();
        assertEquals(12, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());

        mockMvc.perform(patch("/api/boss/part-requests/{id}/receive", requestId)
                        .header("Authorization", bearer(fixture.bossToken())))
                .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();
        assertEquals(12, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());
    }

    @Test
    void rejectedRequestCannotBeApprovedOrReceivedAndDoesNotChangeStock() throws Exception {
        Fixture fixture = createFixture(7);
        Long requestId = createRequest(fixture, 5);

        mockMvc.perform(patch("/api/boss/part-requests/{id}/reject", requestId)
                        .header("Authorization", bearer(fixture.bossToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(patch("/api/boss/part-requests/{id}/approve", requestId)
                        .header("Authorization", bearer(fixture.bossToken())))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/boss/part-requests/{id}/receive", requestId)
                        .header("Authorization", bearer(fixture.bossToken())))
                .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();
        assertEquals(7, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());
        assertEquals(PartRequestStatus.REJECTED,
                requestRepository.findById(requestId).orElseThrow().getStatus());
    }

    private Long createRequest(Fixture fixture, int quantity) throws Exception {
        String body = """
                {"partId": %d, "quantity": %d, "note": "  Integration stock request  "}
                """.formatted(fixture.partId(), quantity);

        mockMvc.perform(post("/api/me/part-requests")
                        .header("Authorization", bearer(fixture.mechanicToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.note").value("Integration stock request"));

        entityManager.flush();
        entityManager.clear();
        var requests = requestRepository.findAllByMechanic_IdOrderByCreatedAtDesc(fixture.mechanicId());
        assertEquals(1, requests.size());
        return requests.getFirst().getId();
    }

    private Fixture createFixture(int stockQty) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User mechanicUser = new User();
        mechanicUser.setEmail("request-" + suffix + "@test.local");
        mechanicUser.setName("Request Test");
        mechanicUser.setRole(UserRole.ROLE_MECHANIC);
        mechanicUser.setPasswordHash("not-used");
        userRepository.save(mechanicUser);

        Mechanic mechanic = new Mechanic();
        mechanic.setUser(mechanicUser);
        mechanic.setFixedSalary(new BigDecimal("1000.00"));
        mechanicRepository.save(mechanic);

        Part part = new Part();
        part.setName("Request part " + suffix);
        part.setPrice(new BigDecimal("25.00"));
        part.setStockQty(stockQty);
        partRepository.save(part);
        entityManager.flush();

        User boss = userRepository.findByEmailIgnoreCase("boss@garage.com").orElseThrow();
        return new Fixture(mechanic.getId(), part.getId(), jwtService.createToken(mechanicUser),
                jwtService.createToken(boss));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(Long mechanicId, Long partId, String mechanicToken, String bossToken) {}
}
