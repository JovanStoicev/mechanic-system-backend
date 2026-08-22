package com.garage.system.job;

import com.garage.system.car.CarRepository;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.part.Part;
import com.garage.system.part.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobPartRepository jobPartRepository;
    private final CarRepository carRepository;
    private final MechanicRepository mechanicRepository;
    private final PartRepository partRepository;

    public JobService(
            JobRepository jobRepository,
            JobPartRepository jobPartRepository,
            CarRepository carRepository,
            MechanicRepository mechanicRepository,
            PartRepository partRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobPartRepository = jobPartRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
        this.partRepository = partRepository;
    }

    @Transactional
    public Job createJob(Long mechanicId, CreateJobCommand cmd) {
        var car = carRepository.findById(cmd.carId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Car not found"));

        var mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));

        jobRepository.findTopByCar_IdOrderByMileageDesc(car.getId()).ifPresent(latest -> {
            if (cmd.mileage() < latest.getMileage()) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Mileage cannot be lower than the latest recorded value: " + latest.getMileage());
            }
        });

        // Aggregate duplicate parts (partId -> totalQty)
        Map<Long, Integer> qtyByPart = new HashMap<>();
        for (var line : cmd.parts()) {
            if (line.qty() <= 0) throw new ResponseStatusException(BAD_REQUEST, "qty must be > 0");
            qtyByPart.merge(line.partId(), line.qty(), Integer::sum);
        }

        // Load parts and validate stock
        Map<Long, Part> partsById = new HashMap<>();
        BigDecimal partsTotal = BigDecimal.ZERO;

        for (var entry : qtyByPart.entrySet()) {
            Long partId = entry.getKey();
            int neededQty = entry.getValue();

            Part part = partRepository.findById(partId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part not found: " + partId));

            if (part.getStockQty() < neededQty) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Not enough stock for part '" + part.getName() + "'. Needed " + neededQty + ", have " + part.getStockQty());
            }

            partsById.put(partId, part);
            partsTotal = partsTotal.add(part.getPrice().multiply(BigDecimal.valueOf(neededQty)));
        }

        BigDecimal labourCost = cmd.labourCost().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = partsTotal.add(labourCost).setScale(2, RoundingMode.HALF_UP);

        Job job = new Job();
        job.setCar(car);
        job.setMechanic(mechanic);
        job.setDescription(cmd.description().trim());
        job.setMileage(cmd.mileage());
        job.setPartsCost(partsTotal.setScale(2, RoundingMode.HALF_UP));
        job.setLabourCost(labourCost);
        job.setTotalCost(totalCost);
        job.setStatus(JobStatus.OPEN);

        jobRepository.save(job);

        // Create job_parts + reduce stock
        for (var entry : qtyByPart.entrySet()) {
            Part part = partsById.get(entry.getKey());
            int neededQty = entry.getValue();

            JobPart jp = new JobPart();
            jp.setJob(job);
            jp.setPart(part);
            jp.setQty(neededQty);
            jobPartRepository.save(jp);

            part.setStockQty(part.getStockQty() - neededQty);
            partRepository.save(part);
        }

        return job;
    }

    @Transactional
    public Job completeJob(Long mechanicId, Long jobId) {
        var job = findOwnedJob(mechanicId, jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(BAD_REQUEST, "Only open jobs can be completed");
        }

        job.setStatus(JobStatus.DONE);
        job.setCompletedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }

    @Transactional
    public Job cancelJob(Long mechanicId, Long jobId) {
        var job = findOwnedJob(mechanicId, jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(BAD_REQUEST, "Only open jobs can be cancelled");
        }

        for (var jobPart : jobPartRepository.findAllByJob_Id(jobId)) {
            var part = jobPart.getPart();
            part.setStockQty(part.getStockQty() + jobPart.getQty());
            partRepository.save(part);
        }

        job.setStatus(JobStatus.CANCELLED);
        return jobRepository.save(job);
    }

    private Job findOwnedJob(Long mechanicId, Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job not found"));
        if (!job.getMechanic().getId().equals(mechanicId)) {
            throw new ResponseStatusException(NOT_FOUND, "Job not found");
        }
        return job;
    }

    public record CreateJobCommand(Long carId, String description, int mileage, BigDecimal labourCost, List<PartLine> parts) {}
    public record PartLine(Long partId, int qty) {}
}
