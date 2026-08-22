package com.garage.system.mechanic;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.job.*;
import com.garage.system.part.*;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import com.garage.system.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MechanicDeletionIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired CarRepository carRepository;
    @Autowired PartRepository partRepository;
    @Autowired JobRepository jobRepository;
    @Autowired JobPartRepository jobPartRepository;
    @Autowired PartRequestRepository partRequestRepository;
    @Autowired EntityManager entityManager;
    @Autowired JwtService jwtService;

    @Test
    void bossDeletionCascadesRelatedDataAndRestoresOpenJobStock() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = new User();
        user.setEmail("delete-" + suffix + "@test.local");
        user.setName("Delete Test");
        user.setRole(UserRole.ROLE_MECHANIC);
        user.setPasswordHash("not-used");
        userRepository.save(user);
        Mechanic mechanic = new Mechanic();
        mechanic.setUser(user);
        mechanic.setFixedSalary(new BigDecimal("1000.00"));
        mechanicRepository.save(mechanic);
        Car car = new Car();
        car.setBrand("Test"); car.setModel("Car"); car.setVin("TESTVIN" + suffix + "01");
        car.setEngineType("Petrol"); car.setHorsePower(100);
        carRepository.save(car);
        Part part = new Part();
        part.setName("Deletion part " + suffix); part.setPrice(new BigDecimal("10.00")); part.setStockQty(3);
        partRepository.save(part);
        Job job = new Job();
        job.setMechanic(mechanic); job.setCar(car); job.setDescription("Open test job"); job.setMileage(1);
        job.setPartsCost(new BigDecimal("20.00")); job.setLabourCost(BigDecimal.ZERO);
        job.setTotalCost(new BigDecimal("20.00")); job.setStatus(JobStatus.OPEN);
        jobRepository.save(job);
        JobPart jobPart = new JobPart();
        jobPart.setJob(job); jobPart.setPart(part); jobPart.setQty(2);
        jobPartRepository.save(jobPart);
        PartRequest request = new PartRequest();
        request.setMechanic(mechanic); request.setPart(part); request.setQuantity(1);
        partRequestRepository.save(request);
        Long mechanicId = mechanic.getId(); Long userId = user.getId(); Long jobId = job.getId(); Long requestId = request.getId(); Long partId = part.getId();
        entityManager.flush();

        User boss = userRepository.findByEmailIgnoreCase("boss@garage.com").orElseThrow();
        String token = jwtService.createToken(boss);
        mockMvc.perform(delete("/api/boss/mechanics/{id}", mechanicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();

        assertFalse(mechanicRepository.existsById(mechanicId));
        assertFalse(userRepository.existsById(userId));
        assertFalse(jobRepository.existsById(jobId));
        assertFalse(partRequestRepository.existsById(requestId));
        assertEquals(5, partRepository.findById(partId).orElseThrow().getStockQty());
    }
}
