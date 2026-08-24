package com.garage.system.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByOrderByFullNameAsc();
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmailIgnoreCase(String email);
}
