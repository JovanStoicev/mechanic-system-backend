package com.garage.system.part;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/part-requests")
public class MechanicPartRequestController {
    private final PartRequestService service;

    public MechanicPartRequestController(PartRequestService service) { this.service = service; }

    @GetMapping
    public List<PartRequestResponse> list(Authentication auth) {
        return service.listForMechanic((Long) auth.getPrincipal()).stream().map(PartRequestResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartRequestResponse create(Authentication auth, @Valid @RequestBody CreatePartRequest body) {
        return PartRequestResponse.from(service.create((Long) auth.getPrincipal(), body.partId(), body.quantity(), body.note()));
    }

    public record CreatePartRequest(@NotNull Long partId, @Positive @Max(10000) int quantity,
                                    @Size(max = 500) String note) {}
}
