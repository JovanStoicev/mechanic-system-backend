package com.garage.system.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobPartRepository extends JpaRepository<JobPart, Long> {
    List<JobPart> findAllByJob_Id(Long jobId);
    void deleteAllByJob_Id(Long jobId);
}
