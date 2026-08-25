package com.garage.system.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByJob_Id(Long jobId);
    List<Invoice> findAllByOrderByIssuedAtDesc();
    List<Invoice> findAllByCustomerUserIdOrderByIssuedAtDesc(Long customerUserId);
    Optional<Invoice> findByIdAndCustomerUserId(Long id, Long customerUserId);
}
