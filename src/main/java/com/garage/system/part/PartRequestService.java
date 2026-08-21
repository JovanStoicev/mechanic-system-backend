package com.garage.system.part;

import com.garage.system.mechanic.MechanicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PartRequestService {
    private final PartRequestRepository requestRepository;
    private final PartRepository partRepository;
    private final MechanicRepository mechanicRepository;

    public PartRequestService(PartRequestRepository requestRepository, PartRepository partRepository,
                              MechanicRepository mechanicRepository) {
        this.requestRepository = requestRepository;
        this.partRepository = partRepository;
        this.mechanicRepository = mechanicRepository;
    }

    @Transactional
    public PartRequest create(Long userId, Long partId, int quantity, String note) {
        var mechanic = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        var part = partRepository.findById(partId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part not found"));
        var request = new PartRequest();
        request.setMechanic(mechanic);
        request.setPart(part);
        request.setQuantity(quantity);
        request.setNote(note == null || note.isBlank() ? null : note.trim());
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<PartRequest> listForMechanic(Long userId) {
        var mechanic = mechanicRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mechanic not found"));
        return requestRepository.findAllByMechanic_IdOrderByCreatedAtDesc(mechanic.getId());
    }

    @Transactional
    public PartRequest decide(Long id, PartRequestStatus decision) {
        var request = requestRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part request not found"));
        if (request.getStatus() != PartRequestStatus.PENDING) {
            throw new ResponseStatusException(CONFLICT, "Part request has already been decided");
        }
        request.setStatus(decision);
        request.setUpdatedAt(OffsetDateTime.now());
        return request;
    }

    @Transactional
    public PartRequest receive(Long id) {
        var request = requestRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part request not found"));
        if (request.getStatus() != PartRequestStatus.APPROVED) {
            throw new ResponseStatusException(CONFLICT, "Only an approved part request can be received");
        }
        var part = partRepository.findLockedById(request.getPart().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part not found"));
        part.setStockQty(Math.addExact(part.getStockQty(), request.getQuantity()));
        request.setStatus(PartRequestStatus.RECEIVED);
        request.setUpdatedAt(OffsetDateTime.now());
        return request;
    }
}
