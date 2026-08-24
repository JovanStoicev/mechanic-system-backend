package com.garage.system.problem;

import com.garage.system.car.CarRepository;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobService;
import com.garage.system.mechanic.MechanicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomerProblemService {
    private final CustomerProblemRepository problemRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final MechanicRepository mechanicRepository;
    private final JobService jobService;
    private final JobRepository jobRepository;

    public CustomerProblemService(CustomerProblemRepository problemRepository, CustomerRepository customerRepository,
                                  CarRepository carRepository, MechanicRepository mechanicRepository,
                                  JobService jobService, JobRepository jobRepository) {
        this.problemRepository = problemRepository;
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
        this.jobService = jobService;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public CustomerProblem create(Long userId, Long carId, Long mechanicId, String description) {
        var customer = customerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        var car = carRepository.findById(carId)
                .filter(found -> found.getCustomer() != null && found.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Car not found"));
        var mechanic = mechanicRepository.findById(mechanicId)
                .filter(found -> found.isActive())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var problem = new CustomerProblem();
        problem.setCustomer(customer);
        problem.setCar(car);
        problem.setMechanic(mechanic);
        problem.setDescription(description.trim());
        return problemRepository.save(problem);
    }

    @Transactional
    public CustomerProblem cancel(Long userId, Long id) {
        var customer = customerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        var problem = problemRepository.findById(id)
                .filter(found -> found.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Problem not found"));
        if (problem.getStatus() != ProblemStatus.SUBMITTED) {
            throw new ResponseStatusException(CONFLICT, "Only submitted problems can be cancelled");
        }
        problem.setStatus(ProblemStatus.CANCELLED);
        problem.setUpdatedAt(OffsetDateTime.now());
        return problem;
    }

    @Transactional
    public CustomerProblem convert(Long mechanicUserId, Long problemId, String description, int mileage,
                                   BigDecimal labourCost, int estimatedMinutes,
                                   List<JobService.PartLine> parts) {
        var mechanic = mechanicRepository.findByUser_Id(mechanicUserId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var problem = problemRepository.findById(problemId)
                .filter(found -> found.getMechanic().getId().equals(mechanic.getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Problem not found"));
        if (problem.getStatus() != ProblemStatus.SUBMITTED) {
            throw new ResponseStatusException(CONFLICT, "Problem has already been processed");
        }
        String jobDescription = description == null || description.isBlank()
                ? problem.getDescription() : description.trim();
        var job = jobService.createJob(mechanic.getId(), new JobService.CreateJobCommand(
                problem.getCar().getId(), jobDescription, mileage, labourCost, parts));
        job.setEstimatedMinutes(estimatedMinutes);
        jobRepository.save(job);
        problem.setJob(job);
        problem.setStatus(ProblemStatus.CONVERTED);
        problem.setUpdatedAt(OffsetDateTime.now());
        return problem;
    }
}
