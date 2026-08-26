package com.garage.system.security;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.customer.Customer;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationService;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnershipIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired CarRepository carRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired NotificationService notificationService;
    @Autowired JwtService jwtService;

    @Test
    void customerCannotUseAnotherCustomersCarsOrNotifications() throws Exception {
        var first = customer("first");
        var second = customer("second");
        var firstCar = car(first.customer(), "FIRST");
        var secondCar = car(second.customer(), "SECOND");
        var mechanic = mechanic();
        var secondNotification = notificationService.create(second.user(), "TEST", "Private", "Private message", null);
        String token = "Bearer " + jwtService.createToken(first.user());

        mockMvc.perform(get("/api/customer/cars/{id}/history", firstCar.getId()).header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customer/cars/{id}/history", secondCar.getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/customer/cars/{id}", secondCar.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(carJson("OTHER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/customer/problems").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carId":%d,"mechanicId":%d,"description":"Not my car", "appointmentAt":"%s"}
                                """.formatted(secondCar.getId(), mechanic.getId(), OffsetDateTime.now().plusDays(2))))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/notifications/{id}/read", secondNotification.getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    private CustomerData customer(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = userRepository.save(user(prefix + "-" + suffix + "@test.local", prefix, UserRole.ROLE_CUSTOMER));
        Customer customer = new Customer(); customer.setUser(user); customer.setFullName(prefix + " Customer");
        customer.setPhone("+381" + suffix); customer.setEmail(user.getEmail());
        return new CustomerData(user, customerRepository.save(customer));
    }

    private Car car(Customer customer, String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Car car = new Car(); car.setCustomer(customer); car.setBrand(prefix); car.setModel("Car");
        car.setVin((prefix + suffix + "00000000000000000").substring(0, 17)); car.setEngineType("Petrol"); car.setHorsePower(100);
        return carRepository.save(car);
    }

    private Mechanic mechanic() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = userRepository.save(user("mechanic-" + suffix + "@test.local", "Mechanic", UserRole.ROLE_MECHANIC));
        Mechanic mechanic = new Mechanic(); mechanic.setUser(user); mechanic.setFixedSalary(BigDecimal.ZERO);
        return mechanicRepository.save(mechanic);
    }

    private User user(String email, String name, UserRole role) {
        User user = new User(); user.setEmail(email); user.setName(name); user.setRole(role); user.setPasswordHash("test"); return user;
    }

    private String carJson(String prefix) {
        return """
                {"brand":"%s","model":"Car","vin":"12345678901234567","engineType":"Petrol","horsePower":100}
                """.formatted(prefix);
    }

    private record CustomerData(User user, Customer customer) {}
}
