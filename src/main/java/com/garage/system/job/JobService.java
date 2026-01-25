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

        BigDecimal totalCost = partsTotal.multiply(BigDecimal.valueOf(1.2))
                .setScale(2, RoundingMode.HALF_UP);

        Job job = new Job();
        job.setCar(car);
        job.setMechanic(mechanic);
        job.setDescription(cmd.description().trim());
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

    public record CreateJobCommand(Long carId, String description, List<PartLine> parts) {}
    public record PartLine(Long partId, int qty) {}
}
