package com.garage.system.problem;

import java.time.OffsetDateTime;

public record ProblemResponse(Long id, Long carId, String carName, String vin, Long mechanicId,
                              String mechanicName, String customerName, String description, String status,
                              Long jobId, Integer estimatedMinutes, String jobStatus,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static ProblemResponse from(CustomerProblem problem) {
        var job = problem.getJob();
        return new ProblemResponse(problem.getId(), problem.getCar().getId(),
                problem.getCar().getBrand() + " " + problem.getCar().getModel(), problem.getCar().getVin(),
                problem.getMechanic().getId(), problem.getMechanic().getUser().getName(),
                problem.getCustomer().getFullName(), problem.getDescription(), problem.getStatus().name(),
                job == null ? null : job.getId(), job == null ? null : job.getEstimatedMinutes(),
                job == null ? null : job.getStatus().name(), problem.getCreatedAt(), problem.getUpdatedAt());
    }
}
