package com.garage.system.dashboard;

import com.garage.system.car.CarRepository;
import com.garage.system.customer.CustomerRepository;
import com.garage.system.finance.FinanceReportService;
import com.garage.system.invoice.InvoiceRepository;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobStatus;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.notification.NotificationRepository;
import com.garage.system.part.PartRequestRepository;
import com.garage.system.part.PartRequestStatus;
import com.garage.system.problem.AppointmentStatus;
import com.garage.system.problem.CustomerProblemRepository;
import com.garage.system.problem.ProblemStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class DashboardController {
    private final JobRepository jobRepository;
    private final PartRequestRepository partRequestRepository;
    private final CustomerProblemRepository problemRepository;
    private final FinanceReportService financeReportService;
    private final MechanicRepository mechanicRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationRepository notificationRepository;

    public DashboardController(JobRepository jobRepository, PartRequestRepository partRequestRepository,
                               CustomerProblemRepository problemRepository, FinanceReportService financeReportService,
                               MechanicRepository mechanicRepository, CustomerRepository customerRepository,
                               CarRepository carRepository, InvoiceRepository invoiceRepository,
                               NotificationRepository notificationRepository) {
        this.jobRepository = jobRepository;
        this.partRequestRepository = partRequestRepository;
        this.problemRepository = problemRepository;
        this.financeReportService = financeReportService;
        this.mechanicRepository = mechanicRepository;
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/api/boss/dashboard")
    public BossDashboard boss() {
        var finance = financeReportService.report(YearMonth.now());
        return new BossDashboard(jobRepository.countByStatus(JobStatus.OPEN),
                partRequestRepository.countByStatus(PartRequestStatus.PENDING),
                problemRepository.countByAppointmentStatusAndAppointmentAtAfter(AppointmentStatus.CONFIRMED, OffsetDateTime.now()),
                finance.completedJobs(), finance.revenue(), finance.estimatedResult(),
                mechanicRepository.count(), customerRepository.count());
    }

    @GetMapping("/api/me/dashboard")
    public MechanicDashboard mechanic(Authentication auth) {
        var mechanic = mechanicRepository.findByUser_Id((Long) auth.getPrincipal())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var problems = problemRepository.findAllByMechanic_IdOrderByCreatedAtDesc(mechanic.getId());
        long assigned = problems.stream().filter(problem -> problem.getStatus() == ProblemStatus.SUBMITTED ||
                problem.getStatus() == ProblemStatus.REJECTED).count();
        return new MechanicDashboard(assigned,
                jobRepository.countByMechanic_IdAndStatus(mechanic.getId(), JobStatus.OPEN),
                problemRepository.countByMechanic_IdAndAppointmentStatusAndAppointmentAtAfter(
                        mechanic.getId(), AppointmentStatus.CONFIRMED, OffsetDateTime.now()),
                partRequestRepository.findAllByMechanic_IdOrderByCreatedAtDesc(mechanic.getId()).stream()
                        .filter(request -> request.getStatus() == PartRequestStatus.PENDING).count(),
                notificationRepository.countByUser_IdAndReadAtIsNull((Long) auth.getPrincipal()));
    }

    @GetMapping("/api/customer/dashboard")
    public CustomerDashboard customer(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        var customer = customerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        var problems = problemRepository.findAllByCustomer_IdOrderByCreatedAtDesc(customer.getId());
        long activeRepairs = problems.stream().filter(problem -> problem.getStatus() != ProblemStatus.CANCELLED &&
                (problem.getJob() == null || (problem.getJob().getStatus() != JobStatus.DONE &&
                        problem.getJob().getStatus() != JobStatus.CANCELLED))).count();
        return new CustomerDashboard(carRepository.findAllByCustomer_IdOrderByBrandAscModelAsc(customer.getId()).size(),
                activeRepairs,
                problemRepository.countByCustomer_IdAndAppointmentStatusAndAppointmentAtAfter(
                        customer.getId(), AppointmentStatus.CONFIRMED, OffsetDateTime.now()),
                invoiceRepository.findAllByCustomerUserIdOrderByIssuedAtDesc(userId).size(),
                notificationRepository.countByUser_IdAndReadAtIsNull(userId));
    }

    public record BossDashboard(long activeJobs, long pendingPartRequests, long upcomingAppointments,
                                int completedJobsThisMonth, BigDecimal revenueThisMonth,
                                BigDecimal estimatedResultThisMonth, long mechanics, long customers) {}
    public record MechanicDashboard(long assignedProblems, long activeJobs, long upcomingAppointments,
                                    long pendingPartRequests, long unreadNotifications) {}
    public record CustomerDashboard(long cars, long activeRepairs, long upcomingAppointments,
                                    long invoices, long unreadNotifications) {}
}
