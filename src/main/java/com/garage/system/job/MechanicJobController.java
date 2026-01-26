package com.garage.system.job;

import com.garage.system.mechanic.MechanicRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/me/jobs")
public class MechanicJobController {

    private final MechanicRepository mechanicRepository;
    private final JobRepository jobRepository;
    private final JobPartRepository jobPartRepository;
    private final JobService jobService;

    public MechanicJobController(
            MechanicRepository mechanicRepository,
            JobRepository jobRepository,
            JobPartRepository jobPartRepository,
            JobService jobService
    ) {

        this.mechanicRepository = mechanicRepository;
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
        this.jobService = jobService;
    }

    @GetMapping
    public List<JobRow> myJobs(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));

        return jobRepository.findAllByMechanic_Id(mech.getId()).stream()
                .map(j -> new JobRow(j.getId(), j.getCar().getId(), j.getDescription(), j.getTotalCost(), j.getStatus().name()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobRow create(Authentication auth, @Valid @RequestBody CreateJobRequest req) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));

        var job = jobService.createJob(mech.getId(),
                new JobService.CreateJobCommand(req.carId(), req.description(), req.parts().stream()
                        .map(p -> new JobService.PartLine(p.partId(), p.qty()))
                        .toList()
                ));

        return new JobRow(job.getId(), job.getCar().getId(), job.getDescription(), job.getTotalCost(), job.getStatus().name());
    }

    public record CreateJobRequest(
            @NotNull Long carId,
            @NotBlank String description,
            @NotEmpty List<PartLineReq> parts
    ) {}

    public record PartLineReq(
            @NotNull Long partId,
            @Positive int qty
    ) {}

    public record JobRow(
            Long id,
            Long carId,
            String description,
            BigDecimal totalCost,
            String status
    ) {}
}
