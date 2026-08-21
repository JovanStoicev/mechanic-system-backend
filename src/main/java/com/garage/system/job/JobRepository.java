package com.garage.system.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findAllByMechanic_Id(Long mechanicId);
    List<Job> findAllByCar_IdOrderByCreatedAtDesc(Long carId);
    Optional<Job> findTopByCar_IdOrderByMileageDesc(Long carId);
}
