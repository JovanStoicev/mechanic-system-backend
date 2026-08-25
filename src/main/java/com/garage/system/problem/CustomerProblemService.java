package com.garage.system.problem;

import com.garage.system.car.CarRepository;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.job.JobService;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomerProblemService {
    private final CustomerProblemRepository problemRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final MechanicRepository mechanicRepository;
    private final JobService jobService;
    private final NotificationService notificationService;

    public CustomerProblemService(CustomerProblemRepository problemRepository, CustomerRepository customerRepository,
                                  CarRepository carRepository, MechanicRepository mechanicRepository,
                                  JobService jobService, NotificationService notificationService) {
        this.problemRepository = problemRepository;
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
        this.jobService = jobService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CustomerProblem create(Long userId, Long carId, Long mechanicId, String description) {
        return create(userId, carId, mechanicId, description, OffsetDateTime.now().plusDays(1));
    }

    @Transactional
    public CustomerProblem create(Long userId, Long carId, Long mechanicId, String description,
                                  OffsetDateTime appointmentAt) {
        validateFutureAppointment(appointmentAt);
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
        problem.setAppointmentAt(appointmentAt);
        problem.setAppointmentStatus(AppointmentStatus.REQUESTED);
        problemRepository.save(problem);
        notificationService.create(mechanic.getUser(), "PROBLEM_ASSIGNED", "New customer problem",
                customer.getFullName() + " requested " + formatAppointment(appointmentAt) + " for " +
                        car.getBrand() + " " + car.getModel() + ".",
                "/mechanic/problems");
        return problem;
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
        problem.setAppointmentStatus(AppointmentStatus.CANCELLED);
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getMechanic().getUser(), "PROBLEM_CANCELLED", "Customer cancelled a problem",
                customer.getFullName() + " cancelled the problem for " + problem.getCar().getBrand() + " " + problem.getCar().getModel() + ".",
                "/mechanic/problems");
        return problem;
    }

    @Transactional
    public CustomerProblem confirmAppointment(Long mechanicUserId, Long problemId) {
        var problem = mechanicProblem(mechanicUserId, problemId);
        if (problem.getAppointmentStatus() != AppointmentStatus.REQUESTED) {
            throw new ResponseStatusException(CONFLICT, "Only requested appointments can be confirmed");
        }
        validateFutureAppointment(problem.getAppointmentAt());
        ensureSlotAvailable(problem);
        problem.setAppointmentStatus(AppointmentStatus.CONFIRMED);
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getCustomer().getUser(), "APPOINTMENT_CONFIRMED",
                "Appointment confirmed", problem.getMechanic().getUser().getName() + " confirmed " +
                        formatAppointment(problem.getAppointmentAt()) + ".", "/customer/problems");
        return problem;
    }

    @Transactional
    public CustomerProblem proposeAppointment(Long mechanicUserId, Long problemId, OffsetDateTime appointmentAt) {
        var problem = mechanicProblem(mechanicUserId, problemId);
        if (problem.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(CONFLICT, "Cancelled appointments cannot be rescheduled");
        }
        validateFutureAppointment(appointmentAt);
        problem.setAppointmentAt(appointmentAt);
        ensureSlotAvailable(problem);
        problem.setAppointmentStatus(AppointmentStatus.PROPOSED);
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getCustomer().getUser(), "APPOINTMENT_PROPOSED",
                "New appointment time proposed", problem.getMechanic().getUser().getName() + " proposed " +
                        formatAppointment(appointmentAt) + ".", "/customer/problems");
        return problem;
    }

    @Transactional
    public CustomerProblem acceptAppointment(Long customerUserId, Long problemId) {
        var problem = ownedProblem(customerUserId, problemId);
        if (problem.getAppointmentStatus() != AppointmentStatus.PROPOSED) {
            throw new ResponseStatusException(CONFLICT, "There is no proposed appointment to accept");
        }
        validateFutureAppointment(problem.getAppointmentAt());
        ensureSlotAvailable(problem);
        problem.setAppointmentStatus(AppointmentStatus.CONFIRMED);
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getMechanic().getUser(), "APPOINTMENT_ACCEPTED",
                "Appointment accepted", problem.getCustomer().getFullName() + " accepted " +
                        formatAppointment(problem.getAppointmentAt()) + ".", "/mechanic/problems");
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
        if (problem.getStatus() != ProblemStatus.SUBMITTED && problem.getStatus() != ProblemStatus.REJECTED) {
            throw new ResponseStatusException(CONFLICT, "Problem is not ready for an estimate");
        }
        String jobDescription = description == null || description.isBlank()
                ? problem.getDescription() : description.trim();
        boolean revised = problem.getJob() != null;
        var command = new JobService.CreateJobCommand(problem.getCar().getId(), jobDescription, mileage, labourCost, parts);
        var job = problem.getJob() == null
                ? jobService.createEstimate(mechanic.getId(), command, estimatedMinutes)
                : jobService.reviseEstimate(mechanic.getId(), problem.getJob().getId(), command, estimatedMinutes);
        problem.setJob(job);
        problem.setStatus(ProblemStatus.ESTIMATE_CREATED);
        problem.setRejectionReason(null);
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getCustomer().getUser(), "ESTIMATE_READY",
                revised ? "Revised estimate ready" : "Estimate ready for review",
                "The mechanic sent an estimate for " + problem.getCar().getBrand() + " " + problem.getCar().getModel() + ".",
                "/customer/problems");
        return problem;
    }

    @Transactional
    public CustomerProblem approve(Long customerUserId, Long problemId) {
        var problem = ownedProblem(customerUserId, problemId);
        if (problem.getStatus() != ProblemStatus.ESTIMATE_CREATED || problem.getJob() == null) {
            throw new ResponseStatusException(CONFLICT, "Estimate is not waiting for approval");
        }
        jobService.approveEstimate(problem.getJob().getId());
        problem.setStatus(ProblemStatus.APPROVED);
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getMechanic().getUser(), "ESTIMATE_APPROVED", "Estimate approved",
                problem.getCustomer().getFullName() + " approved the estimate for " + problem.getCar().getBrand() + " " + problem.getCar().getModel() + ".",
                "/mechanic/jobs");
        return problem;
    }

    @Transactional
    public CustomerProblem reject(Long customerUserId, Long problemId, String reason) {
        var problem = ownedProblem(customerUserId, problemId);
        if (problem.getStatus() != ProblemStatus.ESTIMATE_CREATED || problem.getJob() == null) {
            throw new ResponseStatusException(CONFLICT, "Estimate is not waiting for approval");
        }
        jobService.rejectEstimate(problem.getJob().getId());
        problem.setStatus(ProblemStatus.REJECTED);
        problem.setRejectionReason(reason.trim());
        problem.setUpdatedAt(OffsetDateTime.now());
        notificationService.create(problem.getMechanic().getUser(), "ESTIMATE_REJECTED", "Estimate rejected",
                problem.getCustomer().getFullName() + " rejected the estimate: " + reason.trim(), "/mechanic/problems");
        return problem;
    }

    private CustomerProblem ownedProblem(Long userId, Long problemId) {
        var customer = customerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        return problemRepository.findById(problemId)
                .filter(found -> found.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Problem not found"));
    }

    private CustomerProblem mechanicProblem(Long userId, Long problemId) {
        var mechanic = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        return problemRepository.findById(problemId)
                .filter(found -> found.getMechanic().getId().equals(mechanic.getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Problem not found"));
    }

    private void validateFutureAppointment(OffsetDateTime appointmentAt) {
        if (appointmentAt == null || !appointmentAt.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Appointment must be in the future");
        }
    }

    private void ensureSlotAvailable(CustomerProblem problem) {
        boolean overlaps = problemRepository.findAllByMechanic_IdAndAppointmentStatus(
                        problem.getMechanic().getId(), AppointmentStatus.CONFIRMED).stream()
                .filter(existing -> !existing.getId().equals(problem.getId()))
                .anyMatch(existing -> Math.abs(Duration.between(existing.getAppointmentAt(),
                        problem.getAppointmentAt()).toMinutes()) < 60);
        if (overlaps) {
            throw new ResponseStatusException(CONFLICT,
                    "The mechanic already has a confirmed appointment within this one-hour slot");
        }
    }

    private String formatAppointment(OffsetDateTime appointmentAt) {
        return "an appointment on " + appointmentAt.toLocalDate() + " at " + appointmentAt.toLocalTime();
    }
}
