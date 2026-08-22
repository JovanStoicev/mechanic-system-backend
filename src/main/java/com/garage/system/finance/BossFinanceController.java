package com.garage.system.finance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.YearMonth;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/boss/finance")
public class BossFinanceController {
    private final FinanceReportService service;

    public BossFinanceController(FinanceReportService service) { this.service = service; }

    @GetMapping
    public FinanceReportService.FinanceReport report(@RequestParam int year, @RequestParam int month) {
        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(BAD_REQUEST, "Year must be between 2000 and 2100");
        }
        try {
            return service.report(YearMonth.of(year, month));
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Month must be between 1 and 12", exception);
        }
    }
}
