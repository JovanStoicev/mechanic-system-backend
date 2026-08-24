package com.garage.system.car;

import com.garage.system.customer.Customer;
import com.garage.system.customer.CustomerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;

@RestController
@RequestMapping("/api/boss/cars")
public class BossCarController {

    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;

    public BossCarController(CarRepository carRepository, CustomerRepository customerRepository) {
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<CarResponse> list() {
        return carRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponse create(@Valid @RequestBody CreateCarRequest req) {
        String vin = req.vin().trim();
        if (carRepository.findByVin(vin).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "VIN already exists");
        }

        Car c = new Car();
        c.setBrand(req.brand().trim());
        c.setModel(req.model().trim());
        c.setVin(vin);
        c.setEngineType(req.engineType().trim());
        c.setHorsePower(req.horsePower());
        c.setCustomer(findCustomer(req.customerId()));

        carRepository.save(c);

        return toResponse(c);
    }

    @PatchMapping("/{id}/customer")
    public CarResponse assignCustomer(@PathVariable Long id, @Valid @RequestBody AssignCustomerRequest req) {
        var car = carRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));
        car.setCustomer(findCustomer(req.customerId()));
        return toResponse(carRepository.save(car));
    }

    private Customer findCustomer(Long customerId) {
        if (customerId == null) return null;
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private CarResponse toResponse(Car car) {
        var customer = car.getCustomer();
        return new CarResponse(car.getId(), car.getBrand(), car.getModel(), car.getVin(), car.getEngineType(),
                car.getHorsePower(), customer == null ? null : customer.getId(),
                customer == null ? null : customer.getFullName());
    }

    public record CreateCarRequest(
            @NotBlank String brand,
            @NotBlank String model,
            @NotBlank @Size(min=17, max=17) String vin,
            @NotBlank String engineType,
            @NotNull @Min(0) Integer horsePower,
            Long customerId
    ) {}

    public record AssignCustomerRequest(Long customerId) {}

    public record CarResponse(
            Long id,
            String brand,
            String model,
            String vin,
            String engineType,
            int horsePower,
            Long customerId,
            String customerName
    ) {}
}
