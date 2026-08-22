package com.garage.system.finance;

import com.garage.system.job.Job;
import com.garage.system.job.JobRepository;
import com.garage.system.part.Part;
import com.garage.system.part.PartRequest;
import com.garage.system.part.PartRequestRepository;
import com.garage.system.salary.SalaryReportService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceReportServiceTests {
    @Mock JobRepository jobRepository;
    @Mock PartRequestRepository partRequestRepository;
    @Mock SalaryReportService salaryReportService;
    @InjectMocks FinanceReportService service;

    @Test
    void calculatesRevenueExpensesAndEstimatedResult() {
        var job = new Job();
        job.setPartsCost(new BigDecimal("100.00"));
        job.setLabourCost(new BigDecimal("50.00"));
        var part = new Part();
        ReflectionTestUtils.setField(part, "id", 3L);
        part.setName("Oil filter");
        part.setPrice(new BigDecimal("10.00"));
        var request = new PartRequest();
        ReflectionTestUtils.setField(request, "id", 4L);
        request.setPart(part);
        request.setQuantity(2);
        request.setUpdatedAt(OffsetDateTime.parse("2026-08-10T10:00:00+02:00"));
        var salaryRow = new SalaryReportService.SalaryRow(1L, "Marko", new BigDecimal("800.00"),
                1, new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("805.00"), List.of());

        when(jobRepository.findAllByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(any(), any(), any()))
                .thenReturn(List.of(job));
        when(partRequestRepository.findAllByStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(any(), any(), any()))
                .thenReturn(List.of(request));
        when(salaryReportService.report(YearMonth.of(2026, 8)))
                .thenReturn(new SalaryReportService.SalaryReport(2026, 8, new BigDecimal("0.10"), List.of(salaryRow)));

        var report = service.report(YearMonth.of(2026, 8));

        assertEquals(new BigDecimal("150.00"), report.revenue());
        assertEquals(new BigDecimal("20.00"), report.inventoryExpenses());
        assertEquals(new BigDecimal("805.00"), report.salaryExpenses());
        assertEquals(new BigDecimal("825.00"), report.totalExpenses());
        assertEquals(new BigDecimal("-675.00"), report.estimatedResult());
    }
}
