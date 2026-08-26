package com.garage.system.job;

import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationService;
import com.garage.system.problem.CustomerProblemRepository;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    @PatchMapping("/{id}/complete")
    public JobRow complete(Authentication auth, @PathVariable Long id,
                           @RequestBody(required = false) CompleteJobRequest request) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var job = jobService.completeJob(mech.getId(), id,
                request == null ? null : request.nextServiceDate(),
                request == null ? null : request.nextServiceMileage());
        var recommendation = job.getNextServiceDate() == null && job.getNextServiceMileage() == null ? "" :
                " Next service: " + (job.getNextServiceDate() == null ? "" : job.getNextServiceDate()) +
                        (job.getNextServiceMileage() == null ? "" : " at " + job.getNextServiceMileage() + " km") + ".";
        notifyCustomer(job, "REPAIR_COMPLETED", "Repair completed",
                "The repair for " + job.getCar().getBrand() + " " + job.getCar().getModel() + " is complete." + recommendation,
                "/customer/cars/" + job.getCar().getId() + "/history");
        return toRow(job);
    }

    @PatchMapping("/{id}/cancel")
    public JobRow cancel(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        var mech = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var job = jobService.cancelJob(mech.getId(), id);
        notifyCustomer(job, "REPAIR_CANCELLED", "Repair cancelled",
                "The repair for " + job.getCar().getBrand() + " " + job.getCar().getModel() + " was cancelled.",
                "/customer/problems");
        return toRow(job);
    }

    private void notifyCustomer(Job job, String type, String title, String message, String link) {
        problemRepository.findByJob_Id(job.getId()).map(problem -> problem.getCustomer().getUser())
                .ifPresent(user -> notificationService.create(user, type, title, message, link));
    }

    private JobRow toRow(Job job) {
        return new JobRow(job.getId(), job.getCar().getId(), job.getDescription(),
                job.getMileage(),
                job.getEstimatedMinutes(), job.getPartsCost(), job.getLabourCost(), job.getTotalCost(), job.getStatus().name(),
                job.getCreatedAt(), job.getCompletedAt(), job.getNextServiceDate(), job.getNextServiceMileage());
    }

    public record CompleteJobRequest(LocalDate nextServiceDate, @Positive Integer nextServiceMileage) {}

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
            java.time.OffsetDateTime completedAt,
            LocalDate nextServiceDate,
            Integer nextServiceMileage
    ) {}
}
