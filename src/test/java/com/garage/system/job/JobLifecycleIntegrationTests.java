package com.garage.system.job;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.part.Part;
import com.garage.system.part.PartRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobLifecycleIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired CarRepository carRepository;
    @Autowired PartRepository partRepository;
    @Autowired JobRepository jobRepository;
    @Autowired EntityManager entityManager;
    @Autowired JwtService jwtService;

    @Test
    void creatingAndCancellingJobReservesThenRestoresStock() throws Exception {
        Fixture fixture = createFixture(5);

        createJob(fixture, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.partsCost").value(40.00))
                .andExpect(jsonPath("$.totalCost").value(90.00));

        entityManager.flush();
        entityManager.clear();
        Job job = onlyJobFor(fixture.mechanicId());
        assertEquals(3, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());

        mockMvc.perform(patch("/api/me/jobs/{id}/cancel", job.getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        entityManager.flush();
        entityManager.clear();
        assertEquals(5, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());
        assertEquals(JobStatus.CANCELLED, jobRepository.findById(job.getId()).orElseThrow().getStatus());

        mockMvc.perform(patch("/api/me/jobs/{id}/complete", job.getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completingJobKeepsPartsConsumedAndPreventsFurtherTransitions() throws Exception {
        Fixture fixture = createFixture(5);
        createJob(fixture, 2).andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
        Job job = onlyJobFor(fixture.mechanicId());

        mockMvc.perform(patch("/api/me/jobs/{id}/complete", job.getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        entityManager.flush();
        entityManager.clear();
        Job completed = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.DONE, completed.getStatus());
        assertTrue(completed.getCompletedAt() != null);
        assertEquals(3, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());

        mockMvc.perform(patch("/api/me/jobs/{id}/complete", job.getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/me/jobs/{id}/cancel", job.getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jobCreationFailsWithoutEnoughStockAndLeavesDatabaseUnchanged() throws Exception {
        Fixture fixture = createFixture(1);

        createJob(fixture, 2)
                .andExpect(status().isBadRequest());

        entityManager.flush();
        entityManager.clear();
        assertEquals(1, partRepository.findById(fixture.partId()).orElseThrow().getStockQty());
        assertTrue(jobRepository.findAllByMechanic_Id(fixture.mechanicId()).isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions createJob(Fixture fixture, int quantity) throws Exception {
        String body = """
                {
                  "carId": %d,
                  "description": "Integration lifecycle job",
                  "mileage": 12345,
                  "labourCost": 50.00,
                  "parts": [{"partId": %d, "qty": %d}]
                }
                """.formatted(fixture.carId(), fixture.partId(), quantity);

        return mockMvc.perform(post("/api/me/jobs")
                .header("Authorization", bearer(fixture.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Fixture createFixture(int stockQty) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        User user = new User();
        user.setEmail("job-" + suffix + "@test.local");
        user.setName("Job Lifecycle Test");
        user.setRole(UserRole.ROLE_MECHANIC);
        user.setPasswordHash("not-used");
        userRepository.save(user);

        Mechanic mechanic = new Mechanic();
        mechanic.setUser(user);
        mechanic.setFixedSalary(new BigDecimal("1000.00"));
        mechanicRepository.save(mechanic);

        Car car = new Car();
        car.setBrand("Test");
        car.setModel("Lifecycle");
        car.setVin("JOBTEST" + suffix + "01");
        car.setEngineType("Petrol");
        car.setHorsePower(100);
        carRepository.save(car);

        Part part = new Part();
        part.setName("Lifecycle part " + suffix);
        part.setPrice(new BigDecimal("20.00"));
        part.setStockQty(stockQty);
        partRepository.save(part);

        entityManager.flush();
        return new Fixture(mechanic.getId(), car.getId(), part.getId(), jwtService.createToken(user));
    }

    private Job onlyJobFor(Long mechanicId) {
        var jobs = jobRepository.findAllByMechanic_Id(mechanicId);
        assertEquals(1, jobs.size());
        return jobs.getFirst();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(Long mechanicId, Long carId, Long partId, String token) {}
}
