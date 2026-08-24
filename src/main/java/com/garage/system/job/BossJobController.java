package com.garage.system.job;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/boss/jobs")
public class BossJobController {

    private final JobRepository jobRepository;
    private final JobPartRepository jobPartRepository;

    public BossJobController(JobRepository jobRepository, JobPartRepository jobPartRepository) {
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
    }

    @GetMapping
    public List<JobBossRow> list() {
        return jobRepository.findAll().stream()
                .map(j -> new JobBossRow(
                        j.getId(),
                        j.getCar().getId(),
                        j.getCar().getBrand(),
                        j.getCar().getModel(),
                        j.getCar().getVin(),
                        j.getMechanic().getId(),
                        j.getMechanic().getUser().getName(),
                        j.getDescription(),
                        j.getMileage(),
                        j.getEstimatedMinutes(),
                        j.getPartsCost(),
                        j.getLabourCost(),
                        j.getTotalCost(),
                        j.getStatus().name(),
                        j.getCreatedAt(),
                        j.getCompletedAt()
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public JobDetails details(@PathVariable Long id) {
        var j = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job not found"));
        var parts = jobPartRepository.findAllByJob_Id(id).stream()
                .map(jp -> new UsedPart(jp.getPart().getId(), jp.getPart().getName(),
                        jp.getPart().getPrice(), jp.getQty(),
                        jp.getPart().getPrice().multiply(BigDecimal.valueOf(jp.getQty()))))
                .toList();
        return new JobDetails(j.getId(), j.getCar().getId(), j.getCar().getBrand(), j.getCar().getModel(),
                j.getCar().getVin(), j.getMechanic().getId(), j.getMechanic().getUser().getName(),
                j.getDescription(), j.getMileage(), j.getPartsCost(), j.getLabourCost(), j.getTotalCost(),
                j.getEstimatedMinutes(),
                j.getStatus().name(), j.getCreatedAt(), j.getCompletedAt(), parts);
    }

    public record JobBossRow(
            Long id,
            Long carId,
            String carBrand,
            String carModel,
            String vin,
            Long mechanicId,
            String mechanicName,
            String description,
            int mileage,
            Integer estimatedMinutes,
            BigDecimal partsCost,
            BigDecimal labourCost,
            BigDecimal totalCost,
            String status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime completedAt
    ) {}

    public record UsedPart(Long partId, String name, BigDecimal unitPrice, int qty, BigDecimal lineTotal) {}
    public record JobDetails(Long id, Long carId, String carBrand, String carModel, String vin,
                             Long mechanicId, String mechanicName, String description,
                             int mileage,
                             BigDecimal partsCost, BigDecimal labourCost, BigDecimal totalCost,
                             Integer estimatedMinutes,
                             String status, java.time.OffsetDateTime createdAt,
                             java.time.OffsetDateTime completedAt, List<UsedPart> parts) {}
}
