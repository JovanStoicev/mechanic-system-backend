package com.garage.system.invoice;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.customer.Customer;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.job.JobService;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InvoiceIntegrationTests {
    @Autowired JobService jobService;
    @Autowired InvoiceService invoiceService;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired InvoicePdfService pdfService;
    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired MechanicRepository mechanicRepository;
    @Autowired CarRepository carRepository;
    @Autowired PartRepository partRepository;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void completingJobCreatesOneImmutableInvoiceSnapshot() {
        var data = setup();
        var job = jobService.createJob(data.mechanic().getId(), new JobService.CreateJobCommand(
                data.car().getId(), "Replace brake component", 125000, new BigDecimal("80.00"),
                List.of(new JobService.PartLine(data.part().getId(), 2))));

        jobService.completeJob(data.mechanic().getId(), job.getId());
        var invoice = invoiceRepository.findByJob_Id(job.getId()).orElseThrow();

        assertEquals("INV-" + job.getCompletedAt().getYear() + "-" + String.format("%06d", job.getId()),
                invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("50.00"), invoice.getPartsCost());
        assertEquals(new BigDecimal("130.00"), invoice.getTotalCost());
        assertEquals(new BigDecimal("25.00"), invoice.getParts().getFirst().getUnitPrice());
        assertEquals("INVOICE_READY", notificationRepository
                .findAllByUser_IdOrderByCreatedAtDesc(data.customerUser().getId()).getFirst().getType());
        var otherCustomer = user("invoice-other-" + UUID.randomUUID() + "@test.local", "Other", UserRole.ROLE_CUSTOMER);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> invoiceService.findOwned(invoice.getId(), otherCustomer.getId()));

        data.part().setPrice(new BigDecimal("99.00"));
        partRepository.save(data.part());
        assertEquals(new BigDecimal("25.00"), invoice.getParts().getFirst().getUnitPrice());

        invoiceService.createForCompletedJob(job);
        assertEquals(1, invoiceRepository.findAllByCustomerUserIdOrderByIssuedAtDesc(
                data.customerUser().getId()).size());
    }

    @Test
    void generatedInvoiceIsAReadablePdfDocument() {
        var data = setup();
        var job = jobService.createJob(data.mechanic().getId(), new JobService.CreateJobCommand(
                data.car().getId(), "Diagnostic inspection", 1000, new BigDecimal("30.00"), List.of()));
        jobService.completeJob(data.mechanic().getId(), job.getId());
        var invoice = invoiceRepository.findByJob_Id(job.getId()).orElseThrow();

        byte[] pdf = pdfService.generate(invoice);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
    }

    private TestData setup() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User customerUser = user("invoice-customer-" + suffix + "@test.local", "Invoice Customer", UserRole.ROLE_CUSTOMER);
        Customer customer = new Customer(); customer.setUser(customerUser); customer.setFullName("Invoice Customer");
        customer.setPhone("+383" + suffix); customer.setEmail(customerUser.getEmail()); customer.setAddress("Test address");
        customerRepository.save(customer);
        User mechanicUser = user("invoice-mechanic-" + suffix + "@test.local", "Invoice Mechanic", UserRole.ROLE_MECHANIC);
        Mechanic mechanic = new Mechanic(); mechanic.setUser(mechanicUser); mechanic.setFixedSalary(BigDecimal.ZERO);
        mechanicRepository.save(mechanic);
        Car car = new Car(); car.setBrand("Test"); car.setModel("Invoice Car");
        car.setVin(("INV" + suffix + "0000").substring(0, 17)); car.setEngineType("Petrol");
        car.setHorsePower(100); car.setCustomer(customer); carRepository.save(car);
        Part part = new Part(); part.setName("Invoice part " + suffix); part.setPrice(new BigDecimal("25.00"));
        part.setStockQty(5); partRepository.save(part);
        return new TestData(customerUser, mechanic, car, part);
    }

    private User user(String email, String name, UserRole role) {
        User user = new User(); user.setEmail(email); user.setName(name); user.setRole(role); user.setPasswordHash("test");
        return userRepository.save(user);
    }

    private record TestData(User customerUser, Mechanic mechanic, Car car, Part part) {}
}
