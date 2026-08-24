package com.garage.system.customer;

import com.garage.system.car.CarRepository;
import com.garage.system.job.JobRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerRow create(@Valid @RequestBody CustomerRequest request) {
        return toRow(customerService.create(toCommand(request)));
    }

    @PutMapping("/{id}")
    public CustomerRow update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return toRow(customerService.update(id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    private CustomerService.CustomerCommand toCommand(CustomerRequest request) {
        return new CustomerService.CustomerCommand(request.fullName(), request.phone(), request.email(), request.address());
    }

    private CustomerRow toRow(Customer customer) {
        return new CustomerRow(customer.getId(), customer.getFullName(), customer.getPhone(), customer.getEmail(),
                customer.getAddress(), carRepository.findAllByCustomer_IdOrderByBrandAscModelAsc(customer.getId()).size());
    }

    public record CustomerRequest(
            @NotBlank @Size(max = 200) String fullName,
            @NotBlank @Size(max = 50) String phone,
            @Email @Size(max = 254) String email,
            @Size(max = 500) String address
    ) {}

    public record CustomerRow(Long id, String fullName, String phone, String email, String address, int carCount) {}
    public record CustomerCar(Long id, String brand, String model, String vin, int jobCount) {}
    public record CustomerDetails(Long id, String fullName, String phone, String email, String address,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt, List<CustomerCar> cars) {}
}
