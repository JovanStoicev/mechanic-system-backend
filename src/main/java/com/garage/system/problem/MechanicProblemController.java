package com.garage.system.problem;

import com.garage.system.job.JobService;
import com.garage.system.job.JobPartRepository;
import com.garage.system.job.JobRepository;
import com.garage.system.mechanic.MechanicRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/me/problems")
public class MechanicProblemController {
    private final CustomerProblemRepository problemRepository;
    private final MechanicRepository mechanicRepository;
    private final CustomerProblemService problemService;
    private final JobPartRepository jobPartRepository;
    private final JobRepository jobRepository;

    public MechanicProblemController(CustomerProblemRepository problemRepository,
                                     MechanicRepository mechanicRepository, CustomerProblemService problemService,
                                     JobPartRepository jobPartRepository, JobRepository jobRepository) {
        this.problemRepository = problemRepository;
        this.mechanicRepository = mechanicRepository;
        this.problemService = problemService;
        this.jobPartRepository = jobPartRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public List<ProblemResponse> list(Authentication auth) {
        var mechanic = mechanicRepository.findByUser_Id((Long) auth.getPrincipal())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        return problemRepository.findAllByMechanic_IdOrderByCreatedAtDesc(mechanic.getId()).stream()
                .map(problem -> ProblemResponse.from(problem, jobPartRepository, jobRepository)).toList();
    }

    @PostMapping("/{id}/job")
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse createJob(Authentication auth, @PathVariable Long id,
                                     @Valid @RequestBody CreateProblemJobRequest request) {
        return ProblemResponse.from(problemService.convert((Long) auth.getPrincipal(), id, request.description(),
                request.mileage(), request.labourCost(), request.estimatedMinutes(), request.parts().stream()
                        .map(line -> new JobService.PartLine(line.partId(), line.qty())).toList()), jobPartRepository,
                jobRepository);
    }

    public record CreateProblemJobRequest(@Size(max = 4000) String description,
                                          @Min(0) int mileage,
                                          @NotNull @PositiveOrZero BigDecimal labourCost,
                                          @Min(1) int estimatedMinutes,
                                          @NotNull List<PartLineRequest> parts) {}
    public record PartLineRequest(@NotNull Long partId, @Positive int qty) {}
}
