package com.garage.system.job;

import com.garage.system.car.CarRepository;
import com.garage.system.part.PartRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/me/catalog")
public class MechanicCatalogController {
    private final CarRepository carRepository;
    private final PartRepository partRepository;

    public MechanicCatalogController(CarRepository carRepository, PartRepository partRepository) {
        this.carRepository = carRepository;
        this.partRepository = partRepository;
    }

    @GetMapping("/cars")
    public List<CarRow> cars() {
        return carRepository.findAll().stream()
                .map(c -> new CarRow(c.getId(), c.getBrand(), c.getModel(), c.getVin()))
                .toList();
    }

    @GetMapping("/parts")
    public List<PartRow> parts() {
        return partRepository.findAll().stream()
                .map(p -> new PartRow(p.getId(), p.getName(), p.getPrice(), p.getStockQty()))
                .toList();
    }

    public record CarRow(Long id, String brand, String model, String vin) {}
    public record PartRow(Long id, String name, BigDecimal price, int stockQty) {}
}
