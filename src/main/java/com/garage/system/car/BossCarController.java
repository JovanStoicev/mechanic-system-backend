package com.garage.system.car;

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

    public BossCarController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @GetMapping
    public List<CarResponse> list() {
        return carRepository.findAll().stream()
                .map(c -> new CarResponse(c.getId(), c.getBrand(), c.getModel(), c.getVin(), c.getEngineType(), c.getHorsePower()))
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

        carRepository.save(c);

        return new CarResponse(c.getId(), c.getBrand(), c.getModel(), c.getVin(), c.getEngineType(), c.getHorsePower());
    }

    public record CreateCarRequest(
            @NotBlank String brand,
            @NotBlank String model,
            @NotBlank @Size(min=17, max=17) String vin,
            @NotBlank String engineType,
            @NotNull @Min(0) Integer horsePower
    ) {}

    public record CarResponse(
            Long id,
            String brand,
            String model,
            String vin,
            String engineType,
            int horsePower
    ) {}
}
