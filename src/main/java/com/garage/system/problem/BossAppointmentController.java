package com.garage.system.problem;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/boss/appointments")
public class BossAppointmentController {
    private final CustomerProblemRepository problemRepository;

    public BossAppointmentController(CustomerProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @GetMapping
    public List<AppointmentResponse> list() {
        return problemRepository.findAllByAppointmentAtIsNotNullOrderByAppointmentAtAsc().stream()
                .map(AppointmentResponse::from).toList();
    }

    public record AppointmentResponse(Long problemId, OffsetDateTime appointmentAt, String status,
                                      String customerName, String mechanicName, String carName, String vin,
                                      String description) {
        static AppointmentResponse from(CustomerProblem problem) {
            return new AppointmentResponse(problem.getId(), problem.getAppointmentAt(),
                    problem.getAppointmentStatus() == null ? null : problem.getAppointmentStatus().name(),
                    problem.getCustomer().getFullName(), problem.getMechanic().getUser().getName(),
                    problem.getCar().getBrand() + " " + problem.getCar().getModel(),
                    problem.getCar().getVin(), problem.getDescription());
        }
    }
}
