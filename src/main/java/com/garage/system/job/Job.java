package com.garage.system.job;

import com.garage.system.car.Car;
import com.garage.system.mechanic.Mechanic;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "jobs")
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private Mechanic mechanic;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int mileage;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @Column(name = "parts_cost", nullable = false)
    private BigDecimal partsCost;

    @Column(name = "labour_cost", nullable = false)
    private BigDecimal labourCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public Mechanic getMechanic() { return mechanic; }
    public String getDescription() { return description; }
    public int getMileage() { return mileage; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getPartsCost() { return partsCost; }
    public BigDecimal getLabourCost() { return labourCost; }
    public JobStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }

    public void setCar(Car car) { this.car = car; }
    public void setMechanic(Mechanic mechanic) { this.mechanic = mechanic; }
    public void setDescription(String description) { this.description = description; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    public void setLabourCost(BigDecimal labourCost) { this.labourCost = labourCost; }
    public void setStatus(JobStatus status) { this.status = status; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
}
