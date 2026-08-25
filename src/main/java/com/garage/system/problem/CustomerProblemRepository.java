package com.garage.system.problem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerProblemRepository extends JpaRepository<CustomerProblem, Long> {
    List<CustomerProblem> findAllByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    List<CustomerProblem> findAllByMechanic_IdOrderByCreatedAtDesc(Long mechanicId);
    List<CustomerProblem> findAllByAppointmentAtIsNotNullOrderByAppointmentAtAsc();
    List<CustomerProblem> findAllByMechanic_IdAndAppointmentStatus(Long mechanicId, AppointmentStatus appointmentStatus);
    java.util.Optional<CustomerProblem> findByJob_Id(Long jobId);
}
