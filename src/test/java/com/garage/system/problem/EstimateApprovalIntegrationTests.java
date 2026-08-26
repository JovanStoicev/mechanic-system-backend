package com.garage.system.problem;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.customer.Customer;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.job.JobService;
import com.garage.system.job.JobStatus;
import com.garage.system.invoice.InvoiceRepository;
import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationRepository;
import com.garage.system.part.Part;
import com.garage.system.part.PartRepository;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EstimateApprovalIntegrationTests {
    @Autowired CustomerProblemService problemService;
    @Autowired CustomerProblemRepository problemRepository;
    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired CarRepository carRepository;
    @Autowired PartRepository partRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JobService jobService;
    @Autowired InvoiceRepository invoiceRepository;

    @Test
    void estimateDoesNotReserveStockUntilCustomerApproves() {
        var data = setup();
        var problem = problemService.create(data.customerUser().getId(), data.car().getId(),
                data.mechanic().getId(), "Engine noise");
        assertEquals("PROBLEM_ASSIGNED", notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                data.mechanicUser().getId()).getFirst().getType());
        problem = problemService.confirmAppointment(data.mechanicUser().getId(), problem.getId());
        assertEquals(AppointmentStatus.CONFIRMED, problem.getAppointmentStatus());

        problem = problemService.convert(data.mechanicUser().getId(), problem.getId(), "Replace component",
                120000, new BigDecimal("80.00"), 2880,
                List.of(new JobService.PartLine(data.part().getId(), 2)));

        assertEquals(ProblemStatus.ESTIMATE_CREATED, problem.getStatus());
        assertEquals(JobStatus.PENDING_APPROVAL, problem.getJob().getStatus());
        assertEquals(5, partRepository.findById(data.part().getId()).orElseThrow().getStockQty());
        assertEquals("ESTIMATE_READY", notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                data.customerUser().getId()).getFirst().getType());

        problem = problemService.approve(data.customerUser().getId(), problem.getId());
        assertEquals(ProblemStatus.APPROVED, problem.getStatus());
        assertEquals(JobStatus.OPEN, problem.getJob().getStatus());
        assertEquals(3, partRepository.findById(data.part().getId()).orElseThrow().getStockQty());
        assertEquals("ESTIMATE_APPROVED", notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                data.mechanicUser().getId()).getFirst().getType());
        Long approvedProblemId = problem.getId();
        assertThrows(ResponseStatusException.class,
                () -> problemService.approve(data.customerUser().getId(), approvedProblemId));

        var nextServiceDate = LocalDate.now().plusMonths(6);
        var completed = jobService.completeJob(data.mechanic().getId(), problem.getJob().getId(),
                nextServiceDate, 130000);
        assertEquals(JobStatus.DONE, completed.getStatus());
        assertEquals(nextServiceDate, completed.getNextServiceDate());
        assertEquals(130000, completed.getNextServiceMileage());
        assertTrue(invoiceRepository.findByJob_Id(completed.getId()).isPresent());
        assertEquals(3, partRepository.findById(data.part().getId()).orElseThrow().getStockQty());
    }

    @Test
    void rejectedEstimateCanBeRevisedWithoutChangingStock() {
        var data = setup();
        var problem = problemService.create(data.customerUser().getId(), data.car().getId(),
                data.mechanic().getId(), "Brake noise");
        problem = problemService.convert(data.mechanicUser().getId(), problem.getId(), null, 50000,
                new BigDecimal("100.00"), 1440, List.of(new JobService.PartLine(data.part().getId(), 1)));

        problem = problemService.reject(data.customerUser().getId(), problem.getId(), "Too expensive");
        assertEquals(ProblemStatus.REJECTED, problem.getStatus());
        assertEquals(JobStatus.REJECTED, problem.getJob().getStatus());
        assertEquals("Too expensive", problem.getRejectionReason());
        assertEquals("ESTIMATE_REJECTED", notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                data.mechanicUser().getId()).getFirst().getType());
        assertEquals(5, partRepository.findById(data.part().getId()).orElseThrow().getStockQty());

        problem = problemService.convert(data.mechanicUser().getId(), problem.getId(), "Revised repair", 50000,
                new BigDecimal("70.00"), 720, List.of(new JobService.PartLine(data.part().getId(), 2)));
        assertEquals(ProblemStatus.ESTIMATE_CREATED, problem.getStatus());
        assertEquals(JobStatus.PENDING_APPROVAL, problem.getJob().getStatus());
        assertNull(problem.getRejectionReason());
        assertEquals(5, partRepository.findById(data.part().getId()).orElseThrow().getStockQty());
    }

    private TestData setup() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User customerUser = user("customer-" + suffix + "@test.local", "Customer", UserRole.ROLE_CUSTOMER);
        Customer customer = new Customer(); customer.setUser(customerUser); customer.setFullName("Customer");
        customer.setPhone("+381" + suffix); customer.setEmail(customerUser.getEmail()); customerRepository.save(customer);
        User mechanicUser = user("mechanic-" + suffix + "@test.local", "Mechanic", UserRole.ROLE_MECHANIC);
        Mechanic mechanic = new Mechanic(); mechanic.setUser(mechanicUser); mechanic.setFixedSalary(BigDecimal.ZERO); mechanicRepository.save(mechanic);
        Car car = new Car(); car.setBrand("Test"); car.setModel("Car"); car.setVin(("VIN" + suffix + "0000").substring(0, 17));
        car.setEngineType("Petrol"); car.setHorsePower(100); car.setCustomer(customer); carRepository.save(car);
        Part part = new Part(); part.setName("Estimate part " + suffix); part.setPrice(new BigDecimal("25.00")); part.setStockQty(5); partRepository.save(part);
        return new TestData(customerUser, mechanicUser, mechanic, car, part);
    }

    private User user(String email, String name, UserRole role) {
        User user = new User(); user.setEmail(email); user.setName(name); user.setRole(role); user.setPasswordHash("test");
        return userRepository.save(user);
    }

    private record TestData(User customerUser, User mechanicUser, Mechanic mechanic, Car car, Part part) {}
}
