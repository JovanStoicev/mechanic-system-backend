package com.garage.system.job;

import com.garage.system.car.Car;
import com.garage.system.car.CarRepository;
import com.garage.system.mechanic.Mechanic;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.part.Part;
import com.garage.system.part.PartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTests {

    @Mock JobRepository jobRepository;
    @Mock JobPartRepository jobPartRepository;
    @Mock CarRepository carRepository;
    @Mock MechanicRepository mechanicRepository;
    @Mock PartRepository partRepository;

    @InjectMocks JobService jobService;

    @Test
    void createsJobCalculatesTotalAndReducesStock() {
        var car = new Car();
        var mechanic = new Mechanic();
        var part = part("Oil filter", "10.00", 5);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(mechanicRepository.findById(2L)).thenReturn(Optional.of(mechanic));
        when(partRepository.findById(3L)).thenReturn(Optional.of(part));

        var result = jobService.createJob(2L, new JobService.CreateJobCommand(
                1L,
                "Oil service",
                new BigDecimal("4.00"),
                List.of(new JobService.PartLine(3L, 1), new JobService.PartLine(3L, 1))
        ));

        assertEquals(new BigDecimal("24.00"), result.getTotalCost());
        assertEquals(3, part.getStockQty());
        assertEquals(JobStatus.OPEN, result.getStatus());
        verify(jobRepository).save(result);
        verify(jobPartRepository).save(any(JobPart.class));
        verify(partRepository).save(part);
    }

    @Test
    void rejectsJobWhenStockIsInsufficient() {
        var part = part("Brake pads", "50.00", 1);

        when(carRepository.findById(1L)).thenReturn(Optional.of(new Car()));
        when(mechanicRepository.findById(2L)).thenReturn(Optional.of(new Mechanic()));
        when(partRepository.findById(3L)).thenReturn(Optional.of(part));

        assertThrows(ResponseStatusException.class, () ->
                jobService.createJob(2L, new JobService.CreateJobCommand(
                        1L,
                        "Brake service",
                        new BigDecimal("20.00"),
                        List.of(new JobService.PartLine(3L, 2))
                ))
        );

        assertEquals(1, part.getStockQty());
        verify(jobRepository, never()).save(any());
        verify(jobPartRepository, never()).save(any());
        verify(partRepository, never()).save(any());
    }

    @Test
    void completesOnlyTheMechanicsOwnOpenJob() {
        var mechanic = new Mechanic();
        ReflectionTestUtils.setField(mechanic, "id", 2L);
        var job = new Job();
        job.setMechanic(mechanic);
        job.setStatus(JobStatus.OPEN);
        when(jobRepository.findById(9L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        var completed = jobService.completeJob(2L, 9L);

        assertEquals(JobStatus.DONE, completed.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(completed.getCompletedAt());
        verify(jobRepository).save(job);
    }

    private Part part(String name, String price, int stock) {
        var part = new Part();
        part.setName(name);
        part.setPrice(new BigDecimal(price));
        part.setStockQty(stock);
        return part;
    }
}
