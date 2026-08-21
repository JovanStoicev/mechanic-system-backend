package com.garage.system.salary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.YearMonth;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/boss/salaries")
public class BossSalaryController {
    private final SalaryReportService salaryReportService;

    public BossSalaryController(SalaryReportService salaryReportService) {
        this.salaryReportService = salaryReportService;
    }

    @GetMapping
    public SalaryReportService.SalaryReport report(@RequestParam int year, @RequestParam int month) {
        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(BAD_REQUEST, "Year must be between 2000 and 2100");
        }
        try {
            return salaryReportService.report(YearMonth.of(year, month));
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Month must be between 1 and 12");
        }
    }
}
