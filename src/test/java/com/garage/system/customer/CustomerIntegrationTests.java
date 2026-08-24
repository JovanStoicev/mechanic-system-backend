package com.garage.system.customer;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.security.JwtService;
import com.garage.system.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired CarRepository carRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;
    @Autowired EntityManager entityManager;

    @Test
    void customerCrudAndCarAssignmentPreserveCarAfterCustomerDeletion() throws Exception {
        String suffix = suffix();
        String phone = "+38160" + suffix;
        String email = "owner-" + suffix + "@TEST.LOCAL";

        mockMvc.perform(post("/api/boss/customers")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("  Ana Owner  ", phone, email, "  Main Street 1  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Ana Owner"))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.address").value("Main Street 1"))
                .andExpect(jsonPath("$.carCount").value(0));

        entityManager.flush();
        Customer customer = customerRepository.findByPhone(phone).orElseThrow();
        Car car = new Car();
        car.setBrand("Test");
        car.setModel("Customer Car");
        car.setVin("CUSTOMR" + suffix + "01");
        car.setEngineType("Hybrid");
        car.setHorsePower(150);
        carRepository.save(car);
        entityManager.flush();

        mockMvc.perform(patch("/api/boss/cars/{id}/customer", car.getId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customer.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.customerName").value("Ana Owner"));

        mockMvc.perform(get("/api/boss/customers/{id}", customer.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cars.length()").value(1))
                .andExpect(jsonPath("$.cars[0].vin").value(car.getVin()));

        mockMvc.perform(put("/api/boss/customers/{id}", customer.getId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Ana Updated", phone, email, "New address")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Updated"));

        mockMvc.perform(delete("/api/boss/customers/{id}", customer.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertTrue(customerRepository.findById(customer.getId()).isEmpty());
        assertNull(carRepository.findById(car.getId()).orElseThrow().getCustomer());
    }

    @Test
    void duplicatePhoneAndEmailAreRejected() throws Exception {
        String suffix = suffix();
        String phone = "+38161" + suffix;
        String email = "duplicate-" + suffix + "@test.local";
        String first = customerJson("First Owner", phone, email, null);

        mockMvc.perform(post("/api/boss/customers")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/boss/customers")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Same Phone", phone, "other-" + email, null)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/boss/customers")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Same Email", phone + "9", email.toUpperCase(), null)))
                .andExpect(status().isConflict());
    }

    private String customerJson(String name, String phone, String email, String address) {
        return """
                {"fullName":"%s","phone":"%s","email":%s,"address":%s}
                """.formatted(name, phone, jsonString(email), jsonString(address));
    }

    private String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private String bearer() {
        var boss = userRepository.findByEmailIgnoreCase("boss@garage.com").orElseThrow();
        return "Bearer " + jwtService.createToken(boss);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
