package com.garage.system.problem;

import com.garage.system.car.Car;
import com.garage.system.customer.Customer;
import com.garage.system.job.Job;
import com.garage.system.mechanic.Mechanic;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_problems")
public class CustomerProblem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(optional = false) @JoinColumn(name = "car_id", nullable = false)
    private Car car;
    @ManyToOne(optional = false) @JoinColumn(name = "mechanic_id", nullable = false)
    private Mechanic mechanic;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProblemStatus status = ProblemStatus.SUBMITTED;
    @OneToOne @JoinColumn(name = "job_id", unique = true)
    private Job job;
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Car getCar() { return car; }
    public Mechanic getMechanic() { return mechanic; }
    public String getDescription() { return description; }
    public ProblemStatus getStatus() { return status; }
    public Job getJob() { return job; }
    public String getRejectionReason() { return rejectionReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public void setCar(Car car) { this.car = car; }
    public void setMechanic(Mechanic mechanic) { this.mechanic = mechanic; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(ProblemStatus status) { this.status = status; }
    public void setJob(Job job) { this.job = job; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
