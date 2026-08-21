package com.garage.system.part;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PartRequestRepository extends JpaRepository<PartRequest, Long> {
    List<PartRequest> findAllByOrderByCreatedAtDesc();
    List<PartRequest> findAllByMechanic_IdOrderByCreatedAtDesc(Long mechanicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from PartRequest request where request.id = :id")
    Optional<PartRequest> findLockedById(Long id);
}
