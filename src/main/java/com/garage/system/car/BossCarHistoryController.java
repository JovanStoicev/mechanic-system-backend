package com.garage.system.car;

import com.garage.system.job.JobPartRepository;
import com.garage.system.job.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/boss/cars")
public class BossCarHistoryController {
    private final CarRepository carRepository;
    private final JobRepository jobRepository;
    private final JobPartRepository jobPartRepository;

    public BossCarHistoryController(CarRepository carRepository, JobRepository jobRepository,
                                    JobPartRepository jobPartRepository) {
        this.carRepository = carRepository;
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
    }

    @GetMapping("/{id}/history")
    public CarHistory history(@PathVariable Long id) {
        var car = carRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Car not found"));

        var jobs = jobRepository.findAllByCar_IdOrderByCreatedAtDesc(id).stream()
                .map(job -> new HistoryJob(
                        job.getId(), job.getMileage(), job.getDescription(),
                        job.getMechanic().getUser().getName(), job.getPartsCost(),
                        job.getLabourCost(), job.getTotalCost(), job.getStatus().name(),
                        job.getCreatedAt(), job.getCompletedAt(),
                        jobPartRepository.findAllByJob_Id(job.getId()).stream()
                                .map(jp -> new UsedPart(jp.getPart().getName(), jp.getQty()))
                                .toList()
                ))
                .toList();

        return new CarHistory(car.getId(), car.getBrand(), car.getModel(), car.getVin(),
                car.getEngineType(), car.getHorsePower(), jobs);
    }

    public record UsedPart(String name, int qty) {}
    public record HistoryJob(Long id, int mileage, String description, String mechanicName,
                             BigDecimal partsCost, BigDecimal labourCost, BigDecimal totalCost,
                             String status, OffsetDateTime createdAt, OffsetDateTime completedAt,
                             List<UsedPart> parts) {}
    public record CarHistory(Long id, String brand, String model, String vin, String engineType,
                             int horsePower, List<HistoryJob> jobs) {}
}
