package com.garage.system.mechanic;

import com.garage.system.job.JobPartRepository;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobStatus;
import com.garage.system.part.PartRepository;
import com.garage.system.part.PartRequestRepository;
import com.garage.system.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.ArrayList;

@Service
public class MechanicDeletionService {
    private final MechanicRepository mechanicRepository;
    private final JobRepository jobRepository;
    private final JobPartRepository jobPartRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final PartRequestRepository partRequestRepository;

    public MechanicDeletionService(MechanicRepository mechanicRepository, JobRepository jobRepository,
                                   JobPartRepository jobPartRepository, PartRepository partRepository,
                                   UserRepository userRepository, PartRequestRepository partRequestRepository) {
        this.mechanicRepository = mechanicRepository;
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
        this.partRepository = partRepository;
        this.userRepository = userRepository;
        this.partRequestRepository = partRequestRepository;
    }

    @Transactional
    public void delete(Long mechanicId) {
        var mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));

        // OPEN jobs only reserved their parts, so deleting them must release that stock.
        var jobs = jobRepository.findAllByMechanic_Id(mechanicId);
        var jobParts = new ArrayList<com.garage.system.job.JobPart>();
        jobs.forEach(job -> {
            var lines = jobPartRepository.findAllByJob_Id(job.getId());
            jobParts.addAll(lines);
            if (job.getStatus() == JobStatus.OPEN) {
                lines.forEach(jobPart -> {
                    var part = partRepository.findLockedById(jobPart.getPart().getId())
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part not found"));
                    part.setStockQty(Math.addExact(part.getStockQty(), jobPart.getQty()));
                });
            }
        });

        Long userId = mechanic.getUser().getId();
        // Flush in dependency order so no managed child still references its deleted parent.
        jobPartRepository.deleteAll(jobParts);
        jobPartRepository.flush();
        jobRepository.deleteAll(jobs);
        jobRepository.flush();
        var partRequests = partRequestRepository.findAllByMechanic_IdOrderByCreatedAtDesc(mechanicId);
        partRequestRepository.deleteAll(partRequests);
        partRequestRepository.flush();
        mechanicRepository.delete(mechanic);
        mechanicRepository.flush();
        userRepository.deleteById(userId);
    }
}
