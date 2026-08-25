package com.garage.system.car;

import com.garage.system.customer.CustomerRepository;
import com.garage.system.invoice.InvoiceRepository;
import com.garage.system.job.JobPartRepository;
import com.garage.system.job.JobRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/customer/cars")
public class CustomerCarHistoryController {
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final JobRepository jobRepository;
    private final JobPartRepository jobPartRepository;
    private final InvoiceRepository invoiceRepository;

    public CustomerCarHistoryController(CustomerRepository customerRepository, CarRepository carRepository,
                                        JobRepository jobRepository, JobPartRepository jobPartRepository,
                                        InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/{id}/history")
    public CarHistory history(Authentication auth, @PathVariable Long id) {
        var customer = customerRepository.findByUser_Id((Long) auth.getPrincipal())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        var car = carRepository.findById(id)
                .filter(found -> found.getCustomer() != null && found.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Car not found"));
        var jobs = jobRepository.findAllByCar_IdOrderByCreatedAtDesc(id).stream()
                .map(job -> new HistoryJob(job.getId(), job.getMileage(), job.getDescription(),
                        job.getMechanic().getUser().getName(), job.getPartsCost(), job.getLabourCost(),
                        job.getTotalCost(), job.getStatus().name(), job.getCreatedAt(), job.getCompletedAt(),
                        job.getNextServiceDate(), job.getNextServiceMileage(),
                        invoiceRepository.findByJob_Id(job.getId()).map(invoice -> invoice.getId()).orElse(null),
                        jobPartRepository.findAllByJob_Id(job.getId()).stream()
                                .map(part -> new UsedPart(part.getPart().getName(), part.getQty())).toList()))
                .toList();
        return new CarHistory(car.getId(), car.getBrand(), car.getModel(), car.getVin(),
                car.getEngineType(), car.getHorsePower(), jobs);
    }

    public record UsedPart(String name, int qty) {}
    public record HistoryJob(Long id, int mileage, String description, String mechanicName,
                             BigDecimal partsCost, BigDecimal labourCost, BigDecimal totalCost,
                             String status, OffsetDateTime createdAt, OffsetDateTime completedAt,
                             LocalDate nextServiceDate, Integer nextServiceMileage, Long invoiceId,
                             List<UsedPart> parts) {}
    public record CarHistory(Long id, String brand, String model, String vin, String engineType,
                             int horsePower, List<HistoryJob> jobs) {}
}
