package com.garage.system.finance;

import com.garage.system.job.Job;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobStatus;
import com.garage.system.part.PartRequestRepository;
import com.garage.system.part.PartRequestStatus;
import com.garage.system.salary.SalaryReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class FinanceReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Belgrade");
    private final JobRepository jobRepository;
    private final PartRequestRepository partRequestRepository;
    private final SalaryReportService salaryReportService;

    public FinanceReportService(JobRepository jobRepository, PartRequestRepository partRequestRepository,
                                SalaryReportService salaryReportService) {
        this.jobRepository = jobRepository;
        this.partRequestRepository = partRequestRepository;
        this.salaryReportService = salaryReportService;
    }

    public FinanceReport report(YearMonth month) {
        OffsetDateTime from = month.atDay(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime to = month.plusMonths(1).atDay(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        List<Job> jobs = jobRepository.findAllByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                JobStatus.DONE, from, to);
        var received = partRequestRepository.findAllByStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                PartRequestStatus.RECEIVED, from, to);
        var salaryReport = salaryReportService.report(month);

        BigDecimal partsRevenue = sum(jobs.stream().map(Job::getPartsCost).toList());
        BigDecimal labourRevenue = sum(jobs.stream().map(Job::getLabourCost).toList());
        BigDecimal revenue = partsRevenue.add(labourRevenue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal inventorySpend = sum(received.stream()
                .map(request -> request.getPart().getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                .toList());
        BigDecimal salarySpend = sum(salaryReport.mechanics().stream()
                .map(SalaryReportService.SalaryRow::totalSalary).toList());
        BigDecimal expenses = inventorySpend.add(salarySpend).setScale(2, RoundingMode.HALF_UP);

        var orders = received.stream().map(request -> new ReceivedOrder(
                request.getId(), request.getPart().getName(), request.getQuantity(), request.getPart().getPrice(),
                request.getPart().getPrice().multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP),
                request.getUpdatedAt())).toList();

        return new FinanceReport(month.getYear(), month.getMonthValue(), jobs.size(), revenue, partsRevenue,
                labourRevenue, salarySpend, inventorySpend, expenses,
                revenue.subtract(expenses).setScale(2, RoundingMode.HALF_UP), orders);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public record ReceivedOrder(Long requestId, String partName, int quantity, BigDecimal unitPrice,
                                BigDecimal total, OffsetDateTime receivedAt) {}
    public record FinanceReport(int year, int month, int completedJobs, BigDecimal revenue,
                                BigDecimal partsRevenue, BigDecimal labourRevenue, BigDecimal salaryExpenses,
                                BigDecimal inventoryExpenses, BigDecimal totalExpenses,
                                BigDecimal estimatedResult, List<ReceivedOrder> receivedOrders) {}
}
