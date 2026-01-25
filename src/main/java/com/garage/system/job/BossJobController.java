package com.garage.system.job;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/boss/jobs")
public class BossJobController {

    private final JobRepository jobRepository;

    public BossJobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public List<JobBossRow> list() {
        return jobRepository.findAll().stream()
                .map(j -> new JobBossRow(
                        j.getId(),
                        j.getCar().getId(),
                        j.getMechanic().getId(),
                        j.getDescription(),
                        j.getTotalCost(),
                        j.getStatus().name()
                ))
                .toList();
    }

    public record JobBossRow(
            Long id,
            Long carId,
            Long mechanicId,
            String description,
            BigDecimal totalCost,
            String status
    ) {}
}
