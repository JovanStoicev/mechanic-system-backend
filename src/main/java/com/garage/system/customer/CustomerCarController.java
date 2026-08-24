package com.garage.system.customer;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/customer/cars")
public class CustomerCarController {
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;

    public CustomerCarController(CustomerRepository customerRepository, CarRepository carRepository) {
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
    }

    @GetMapping
    public List<CarResponse> list(Authentication auth) {
        var customer = customer(auth);
        return carRepository.findAllByCustomer_IdOrderByBrandAscModelAsc(customer.getId()).stream()
                .map(this::response).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponse create(Authentication auth, @Valid @RequestBody CarRequest request) {
        String vin = request.vin().trim();
        if (carRepository.findByVin(vin).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "VIN already exists");
        }
        var car = new Car();
        car.setCustomer(customer(auth));
        apply(car, request, vin);
        return response(carRepository.save(car));
    }

    @PutMapping("/{id}")
    public CarResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody CarRequest request) {
        var customer = customer(auth);
        var car = ownedCar(customer.getId(), id);
        String vin = request.vin().trim();
        carRepository.findByVin(vin).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ResponseStatusException(CONFLICT, "VIN already exists"); });
        apply(car, request, vin);
        return response(carRepository.save(car));
    }

    private Customer customer(Authentication auth) {
        return customerRepository.findByUser_Id((Long) auth.getPrincipal())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
    }

    private Car ownedCar(Long customerId, Long carId) {
        return carRepository.findById(carId)
                .filter(car -> car.getCustomer() != null && car.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Car not found"));
    }

    private void apply(Car car, CarRequest request, String vin) {
        car.setBrand(request.brand().trim());
        car.setModel(request.model().trim());
        car.setVin(vin);
        car.setEngineType(request.engineType().trim());
        car.setHorsePower(request.horsePower());
    }

    private CarResponse response(Car car) {
        return new CarResponse(car.getId(), car.getBrand(), car.getModel(), car.getVin(), car.getEngineType(),
                car.getHorsePower());
    }

    public record CarRequest(@NotBlank @Size(max = 100) String brand,
                             @NotBlank @Size(max = 100) String model,
                             @NotBlank @Size(min = 17, max = 17) String vin,
                             @NotBlank @Size(max = 100) String engineType,
                             @NotNull @Min(0) Integer horsePower) {}
    public record CarResponse(Long id, String brand, String model, String vin, String engineType, int horsePower) {}
}
