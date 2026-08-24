package com.garage.system.customer;

import com.garage.system.car.CarRepository;
import com.garage.system.job.JobRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/boss/customers")
public class BossCustomerController {
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final CarRepository carRepository;
    private final JobRepository jobRepository;

    public BossCustomerController(CustomerRepository customerRepository, CustomerService customerService,
                                  CarRepository carRepository, JobRepository jobRepository) {
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.carRepository = carRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public List<CustomerRow> list() {
        return customerRepository.findAllByOrderByFullNameAsc().stream().map(this::toRow).toList();
    }

    @GetMapping("/{id}")
    public CustomerDetails details(@PathVariable Long id) {
        var customer = customerService.find(id);
        var cars = carRepository.findAllByCustomer_IdOrderByBrandAscModelAsc(id).stream()
                .map(car -> new CustomerCar(car.getId(), car.getBrand(), car.getModel(), car.getVin(),
                        jobRepository.findAllByCar_IdOrderByCreatedAtDesc(car.getId()).size()))
                .toList();
        return new CustomerDetails(customer.getId(), customer.getFullName(), customer.getPhone(),
                customer.getEmail(), customer.getAddress(), customer.getCreatedAt(),
                customer.getUpdatedAt(), cars);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    private CustomerRow toRow(Customer customer) {
        return new CustomerRow(customer.getId(), customer.getFullName(), customer.getPhone(), customer.getEmail(),
                customer.getAddress(), carRepository.findAllByCustomer_IdOrderByBrandAscModelAsc(customer.getId()).size());
    }

    public record CustomerRow(Long id, String fullName, String phone, String email, String address, int carCount) {}
    public record CustomerCar(Long id, String brand, String model, String vin, int jobCount) {}
    public record CustomerDetails(Long id, String fullName, String phone, String email, String address,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt, List<CustomerCar> cars) {}
}
