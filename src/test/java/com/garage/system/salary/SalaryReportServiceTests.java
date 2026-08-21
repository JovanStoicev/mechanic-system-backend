package com.garage.system.salary;

import com.garage.system.car.Car;
import com.garage.system.job.Job;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobStatus;
import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryReportServiceTests {
    @Mock MechanicRepository mechanicRepository;
    @Mock JobRepository jobRepository;
    @InjectMocks SalaryReportService service;

    @Test
    void calculatesFixedSalaryAndTenPercentCompletedLabourBonus() {
        var mechanic = mechanic(1L, "Marko", "800.00");
        var jobOne = completedJob(mechanic, 11L, "100.00");
        var jobTwo = completedJob(mechanic, 12L, "50.00");
        when(mechanicRepository.findAll()).thenReturn(List.of(mechanic));
        when(jobRepository.findAllByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                any(), any(), any())).thenReturn(List.of(jobOne, jobTwo));

        var report = service.report(YearMonth.of(2026, 8));
        var row = report.mechanics().getFirst();

        assertEquals(2, row.completedJobs());
        assertEquals(new BigDecimal("150.00"), row.labourTotal());
        assertEquals(new BigDecimal("15.00"), row.bonus());
        assertEquals(new BigDecimal("815.00"), row.totalSalary());
        verify(jobRepository).findAllByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                org.mockito.ArgumentMatchers.eq(JobStatus.DONE), any(), any());
    }

    @Test
    void mechanicWithoutCompletedJobsReceivesOnlyFixedSalary() {
        var mechanic = mechanic(2L, "Ivan", "900.00");
        when(mechanicRepository.findAll()).thenReturn(List.of(mechanic));
        when(jobRepository.findAllByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                any(), any(), any())).thenReturn(List.of());

        var row = service.report(YearMonth.of(2026, 8)).mechanics().getFirst();

        assertEquals(0, row.completedJobs());
        assertEquals(new BigDecimal("0.00"), row.bonus());
        assertEquals(new BigDecimal("900.00"), row.totalSalary());
    }

    private Mechanic mechanic(Long id, String name, String fixedSalary) {
        var user = new User();
        user.setName(name);
        var mechanic = new Mechanic();
        ReflectionTestUtils.setField(mechanic, "id", id);
        mechanic.setUser(user);
        mechanic.setFixedSalary(new BigDecimal(fixedSalary));
        return mechanic;
    }

    private Job completedJob(Mechanic mechanic, Long id, String labourCost) {
        var car = new Car();
        car.setBrand("Volkswagen");
        car.setModel("Golf");
        var job = new Job();
        ReflectionTestUtils.setField(job, "id", id);
        job.setMechanic(mechanic);
        job.setCar(car);
        job.setDescription("Service");
        job.setLabourCost(new BigDecimal(labourCost));
        job.setStatus(JobStatus.DONE);
        job.setCompletedAt(OffsetDateTime.parse("2026-08-15T12:00:00+02:00"));
        return job;
    }
}
