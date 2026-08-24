package com.garage.system.problem;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import com.garage.system.job.JobPartRepository;

public record ProblemResponse(Long id, Long carId, String carName, String vin, Long mechanicId,
                              String mechanicName, String customerName, String description, String status,
                              Long jobId, Integer estimatedMinutes, String jobStatus, BigDecimal partsCost,
                              BigDecimal workPrice, BigDecimal totalCost, String rejectionReason, List<EstimatePart> parts,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static ProblemResponse from(CustomerProblem problem, JobPartRepository jobPartRepository) {
        var job = problem.getJob();
        var parts = job == null ? List.<EstimatePart>of() : jobPartRepository.findAllByJob_Id(job.getId()).stream()
                .map(line -> new EstimatePart(line.getPart().getId(), line.getPart().getName(),
                        line.getPart().getPrice(), line.getQty())).toList();
        return new ProblemResponse(problem.getId(), problem.getCar().getId(),
                problem.getCar().getBrand() + " " + problem.getCar().getModel(), problem.getCar().getVin(),
                problem.getMechanic().getId(), problem.getMechanic().getUser().getName(),
                problem.getCustomer().getFullName(), problem.getDescription(), problem.getStatus().name(),
                job == null ? null : job.getId(), job == null ? null : job.getEstimatedMinutes(),
                job == null ? null : job.getStatus().name(), job == null ? null : job.getPartsCost(),
                job == null ? null : job.getLabourCost(), job == null ? null : job.getTotalCost(),
                problem.getRejectionReason(), parts, problem.getCreatedAt(), problem.getUpdatedAt());
    }
    public record EstimatePart(Long partId, String name, BigDecimal unitPrice, int qty) {}
}
