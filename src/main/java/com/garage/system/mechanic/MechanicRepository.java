package com.garage.system.mechanic;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MechanicRepository extends JpaRepository<Mechanic, Long> {
    Optional <Mechanic> findByUser_Id(Long userId);
}
