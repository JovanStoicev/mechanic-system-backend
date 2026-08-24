package com.garage.system.problem;

import com.garage.system.customer.CustomerRepository;
import com.garage.system.job.JobPartRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/customer/problems")
public class CustomerProblemController {
    private final CustomerProblemRepository problemRepository;
    private final CustomerRepository customerRepository;
    private final CustomerProblemService problemService;
    private final JobPartRepository jobPartRepository;

    public CustomerProblemController(CustomerProblemRepository problemRepository,
                                     CustomerRepository customerRepository, CustomerProblemService problemService,
                                     JobPartRepository jobPartRepository) {
        this.problemRepository = problemRepository;
        this.customerRepository = customerRepository;
        this.problemService = problemService;
        this.jobPartRepository = jobPartRepository;
    }

    @GetMapping
    public List<ProblemResponse> list(Authentication auth) {
        var customer = customerRepository.findByUser_Id((Long) auth.getPrincipal())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        return problemRepository.findAllByCustomer_IdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(problem -> ProblemResponse.from(problem, jobPartRepository)).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse create(Authentication auth, @Valid @RequestBody CreateProblemRequest request) {
        return response(problemService.create((Long) auth.getPrincipal(), request.carId(), request.mechanicId(), request.description()));
    }

    @PatchMapping("/{id}/cancel")
    public ProblemResponse cancel(Authentication auth, @PathVariable Long id) {
        return response(problemService.cancel((Long) auth.getPrincipal(), id));
    }

    @PatchMapping("/{id}/approve")
    public ProblemResponse approve(Authentication auth, @PathVariable Long id) {
        return response(problemService.approve((Long) auth.getPrincipal(), id));
    }

    @PatchMapping("/{id}/reject")
    public ProblemResponse reject(Authentication auth, @PathVariable Long id,
                                  @Valid @RequestBody RejectEstimateRequest request) {
        return response(problemService.reject((Long) auth.getPrincipal(), id, request.reason()));
    }

    private ProblemResponse response(CustomerProblem problem) { return ProblemResponse.from(problem, jobPartRepository); }

    public record CreateProblemRequest(@NotNull Long carId, @NotNull Long mechanicId,
                                       @NotBlank @Size(max = 4000) String description) {}
    public record RejectEstimateRequest(@NotBlank @Size(max = 1000) String reason) {}
}
