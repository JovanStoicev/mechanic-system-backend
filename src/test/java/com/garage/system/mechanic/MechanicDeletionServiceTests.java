package com.garage.system.mechanic;

import com.garage.system.job.Job;
import com.garage.system.job.JobPart;
import com.garage.system.job.JobPartRepository;
import com.garage.system.job.JobRepository;
import com.garage.system.job.JobStatus;
import com.garage.system.part.Part;
import com.garage.system.part.PartRepository;
import com.garage.system.part.PartRequestRepository;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MechanicDeletionServiceTests {
    @Mock MechanicRepository mechanicRepository;
    @Mock JobRepository jobRepository;
    @Mock JobPartRepository jobPartRepository;
    @Mock PartRepository partRepository;
    @Mock UserRepository userRepository;
    @Mock PartRequestRepository partRequestRepository;
    @InjectMocks MechanicDeletionService service;

    @Test
    void deletesUserAndReleasesPartsReservedByOpenJobs() {
        var user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        var mechanic = new Mechanic();
        mechanic.setUser(user);
        var job = new Job();
        ReflectionTestUtils.setField(job, "id", 8L);
        job.setStatus(JobStatus.OPEN);
        var part = new Part();
        ReflectionTestUtils.setField(part, "id", 4L);
        part.setStockQty(3);
        var jobPart = new JobPart();
        jobPart.setPart(part);
        jobPart.setQty(2);
        jobPart.setUnitPrice(part.getPrice());
        when(mechanicRepository.findById(2L)).thenReturn(Optional.of(mechanic));
        when(jobRepository.findAllByMechanic_Id(2L)).thenReturn(List.of(job));
        when(jobPartRepository.findAllByJob_Id(8L)).thenReturn(List.of(jobPart));
        when(partRepository.findLockedById(4L)).thenReturn(Optional.of(part));

        service.delete(2L);

        assertEquals(5, part.getStockQty());
        verify(jobPartRepository).deleteAll(List.of(jobPart));
        verify(jobPartRepository).flush();
        verify(jobRepository).deleteAll(List.of(job));
        verify(jobRepository).flush();
        verify(mechanicRepository).delete(mechanic);
        verify(mechanicRepository).flush();
        verify(userRepository).deleteById(7L);
    }
}
