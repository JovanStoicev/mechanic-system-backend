package com.garage.system.problem;

import com.garage.system.customer.CustomerRepository;
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

    public CustomerProblemController(CustomerProblemRepository problemRepository,
                                     CustomerRepository customerRepository, CustomerProblemService problemService) {
        this.problemRepository = problemRepository;
        this.customerRepository = customerRepository;
        this.problemService = problemService;
    }

    @GetMapping
    public List<ProblemResponse> list(Authentication auth) {
        var customer = customerRepository.findByUser_Id((Long) auth.getPrincipal())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        return problemRepository.findAllByCustomer_IdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(ProblemResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse create(Authentication auth, @Valid @RequestBody CreateProblemRequest request) {
        return ProblemResponse.from(problemService.create((Long) auth.getPrincipal(), request.carId(),
                request.mechanicId(), request.description()));
    }

    @PatchMapping("/{id}/cancel")
    public ProblemResponse cancel(Authentication auth, @PathVariable Long id) {
        return ProblemResponse.from(problemService.cancel((Long) auth.getPrincipal(), id));
    }

    public record CreateProblemRequest(@NotNull Long carId, @NotNull Long mechanicId,
                                       @NotBlank @Size(max = 4000) String description) {}
}
