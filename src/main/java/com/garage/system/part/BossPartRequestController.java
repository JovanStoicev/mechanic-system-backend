package com.garage.system.part;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boss/part-requests")
public class BossPartRequestController {
    private final PartRequestRepository repository;
    private final PartRequestService service;

    public BossPartRequestController(PartRequestRepository repository, PartRequestService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    public List<PartRequestResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(PartRequestResponse::from).toList();
    }

    @PatchMapping("/{id}/approve")
    public PartRequestResponse approve(@PathVariable Long id) {
        return PartRequestResponse.from(service.decide(id, PartRequestStatus.APPROVED));
    }

    @PatchMapping("/{id}/reject")
    public PartRequestResponse reject(@PathVariable Long id) {
        return PartRequestResponse.from(service.decide(id, PartRequestStatus.REJECTED));
    }
}
