package com.garage.system.problem;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.customer.Customer;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationRepository;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppointmentIntegrationTests {
    @Autowired CustomerProblemService problemService;
    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired CarRepository carRepository;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void mechanicCanConfirmCustomerRequestedAppointment() {
        var data = setup();
        var requestedAt = OffsetDateTime.now().plusDays(2).withMinute(0).withSecond(0).withNano(0);
        var problem = problemService.create(data.customerUser().getId(), data.car().getId(),
                data.mechanic().getId(), "Inspection", requestedAt);

        assertEquals(AppointmentStatus.REQUESTED, problem.getAppointmentStatus());
        problem = problemService.confirmAppointment(data.mechanicUser().getId(), problem.getId());

        assertEquals(AppointmentStatus.CONFIRMED, problem.getAppointmentStatus());
        assertEquals(requestedAt, problem.getAppointmentAt());
        assertEquals("APPOINTMENT_CONFIRMED", notificationRepository
                .findAllByUser_IdOrderByCreatedAtDesc(data.customerUser().getId()).getFirst().getType());
    }

    @Test
    void customerCanAcceptMechanicsProposedTime() {
        var data = setup();
        var problem = problemService.create(data.customerUser().getId(), data.car().getId(),
                data.mechanic().getId(), "Inspection", OffsetDateTime.now().plusDays(2));
        var proposedAt = OffsetDateTime.now().plusDays(3).withMinute(0).withSecond(0).withNano(0);

        problem = problemService.proposeAppointment(data.mechanicUser().getId(), problem.getId(), proposedAt);
        assertEquals(AppointmentStatus.PROPOSED, problem.getAppointmentStatus());
        problem = problemService.acceptAppointment(data.customerUser().getId(), problem.getId());

        assertEquals(AppointmentStatus.CONFIRMED, problem.getAppointmentStatus());
        assertEquals(proposedAt, problem.getAppointmentAt());
        assertEquals("APPOINTMENT_ACCEPTED", notificationRepository
                .findAllByUser_IdOrderByCreatedAtDesc(data.mechanicUser().getId()).getFirst().getType());
    }

    @Test
    void confirmedOneHourSlotsCannotOverlapForSameMechanic() {
        var data = setup();
        var firstTime = OffsetDateTime.now().plusDays(4).withMinute(0).withSecond(0).withNano(0);
        var first = problemService.create(data.customerUser().getId(), data.car().getId(),
                data.mechanic().getId(), "First", firstTime);
        problemService.confirmAppointment(data.mechanicUser().getId(), first.getId());
        var second = problemService.create(data.customerUser().getId(), data.car().getId(),
                data.mechanic().getId(), "Second", firstTime.plusMinutes(30));

        assertThrows(ResponseStatusException.class,
                () -> problemService.confirmAppointment(data.mechanicUser().getId(), second.getId()));
    }

    private TestData setup() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User customerUser = user("appointment-customer-" + suffix + "@test.local", "Customer", UserRole.ROLE_CUSTOMER);
        Customer customer = new Customer(); customer.setUser(customerUser); customer.setFullName("Customer");
        customer.setPhone("+382" + suffix); customer.setEmail(customerUser.getEmail()); customerRepository.save(customer);
        User mechanicUser = user("appointment-mechanic-" + suffix + "@test.local", "Mechanic", UserRole.ROLE_MECHANIC);
        Mechanic mechanic = new Mechanic(); mechanic.setUser(mechanicUser); mechanic.setFixedSalary(BigDecimal.ZERO); mechanicRepository.save(mechanic);
        Car car = new Car(); car.setBrand("Test"); car.setModel("Car"); car.setVin(("APT" + suffix + "0000").substring(0, 17));
        car.setEngineType("Petrol"); car.setHorsePower(100); car.setCustomer(customer); carRepository.save(car);
        return new TestData(customerUser, mechanicUser, mechanic, car);
    }

    private User user(String email, String name, UserRole role) {
        User user = new User(); user.setEmail(email); user.setName(name); user.setRole(role); user.setPasswordHash("test");
        return userRepository.save(user);
    }

    private record TestData(User customerUser, User mechanicUser, Mechanic mechanic, Car car) {}
}
