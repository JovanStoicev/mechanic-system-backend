package com.garage.system.salary;

import com.garage.system.job.Job;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobStatus;
import com.garage.system.mechanic.MechanicRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalaryReportService {
    private static final BigDecimal BONUS_RATE = new BigDecimal("0.10");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Belgrade");

    private final MechanicRepository mechanicRepository;
    private final JobRepository jobRepository;

    public SalaryReportService(MechanicRepository mechanicRepository, JobRepository jobRepository) {
        this.mechanicRepository = mechanicRepository;
        this.jobRepository = jobRepository;
    }

    public SalaryReport report(YearMonth month) {
        OffsetDateTime from = month.atDay(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime to = month.plusMonths(1).atDay(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        Map<Long, List<Job>> jobsByMechanic = jobRepository
                .findAllByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(JobStatus.DONE, from, to)
                .stream()
                .collect(Collectors.groupingBy(job -> job.getMechanic().getId()));

        var rows = mechanicRepository.findAll().stream().map(mechanic -> {
            var jobs = jobsByMechanic.getOrDefault(mechanic.getId(), List.of());
            BigDecimal labourTotal = jobs.stream()
                    .map(Job::getLabourCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal bonus = labourTotal.multiply(BONUS_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal fixedSalary = mechanic.getFixedSalary().setScale(2, RoundingMode.HALF_UP);
            var details = jobs.stream()
                    .map(job -> new SalaryJob(job.getId(), job.getCar().getBrand(), job.getCar().getModel(),
                            job.getDescription(), job.getLabourCost(), job.getCompletedAt()))
                    .toList();
            return new SalaryRow(mechanic.getId(), mechanic.getUser().getName(), fixedSalary,
                    jobs.size(), labourTotal, bonus, fixedSalary.add(bonus), details);
        }).toList();

        return new SalaryReport(month.getYear(), month.getMonthValue(), BONUS_RATE, rows);
    }

    public record SalaryJob(Long jobId, String carBrand, String carModel, String description,
                            BigDecimal labourCost, OffsetDateTime completedAt) {}
    public record SalaryRow(Long mechanicId, String mechanicName, BigDecimal fixedSalary,
                            int completedJobs, BigDecimal labourTotal, BigDecimal bonus,
                            BigDecimal totalSalary, List<SalaryJob> jobs) {}
    public record SalaryReport(int year, int month, BigDecimal bonusRate, List<SalaryRow> mechanics) {}
}
