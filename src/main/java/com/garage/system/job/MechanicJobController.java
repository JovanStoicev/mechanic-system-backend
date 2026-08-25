package com.garage.system.job;

import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationService;
import com.garage.system.problem.CustomerProblemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Min;
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
    private final CustomerProblemRepository problemRepository;
    private final NotificationService notificationService;

    public MechanicJobController(
            MechanicRepository mechanicRepository,
            JobRepository jobRepository,
            JobPartRepository jobPartRepository,
            JobService jobService,
            CustomerProblemRepository problemRepository,
            NotificationService notificationService
    ) {

        this.mechanicRepository = mechanicRepository;
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
        this.jobService = jobService;
        this.problemRepository = problemRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<JobRow> myJobs(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));

        return jobRepository.findAllByMechanic_Id(mech.getId()).stream()
                .map(this::toRow)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobRow create(Authentication auth, @Valid @RequestBody CreateJobRequest req) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));

        var job = jobService.createJob(mech.getId(),
                new JobService.CreateJobCommand(req.carId(), req.description(), req.mileage(), req.labourCost(), req.parts().stream()
                        .map(p -> new JobService.PartLine(p.partId(), p.qty()))
                        .toList()
                ));

        return toRow(job);
    }

    @PatchMapping("/{id}/complete")
    public JobRow complete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var job = jobService.completeJob(mech.getId(), id);
        notifyCustomer(job, "REPAIR_COMPLETED", "Repair completed",
                "The repair for " + job.getCar().getBrand() + " " + job.getCar().getModel() + " is complete.");
        return toRow(job);
    }

    @PatchMapping("/{id}/cancel")
    public JobRow cancel(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var job = jobService.cancelJob(mech.getId(), id);
        notifyCustomer(job, "REPAIR_CANCELLED", "Repair cancelled",
                "The repair for " + job.getCar().getBrand() + " " + job.getCar().getModel() + " was cancelled.");
        return toRow(job);
    }

    private void notifyCustomer(Job job, String type, String title, String message) {
        problemRepository.findByJob_Id(job.getId()).map(problem -> problem.getCustomer().getUser())
                .ifPresent(user -> notificationService.create(user, type, title, message, "/customer/problems"));
    }

    private JobRow toRow(Job job) {
        return new JobRow(job.getId(), job.getCar().getId(), job.getDescription(),
                job.getMileage(),
                job.getEstimatedMinutes(), job.getPartsCost(), job.getLabourCost(), job.getTotalCost(), job.getStatus().name(),
                job.getCreatedAt(), job.getCompletedAt());
    }

    public record CreateJobRequest(
            @NotNull Long carId,
            @NotBlank String description,
            @Min(0) int mileage,
            @NotNull @PositiveOrZero BigDecimal labourCost,
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
            int mileage,
            Integer estimatedMinutes,
            BigDecimal partsCost,
            BigDecimal labourCost,
            BigDecimal totalCost,
            String status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime completedAt
    ) {}
}
