package com.garage.system.part;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/boss/parts")
public class BossPartController {

    private final PartRepository partRepository;

    public BossPartController(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @GetMapping
    public List<PartResponse> list() {
        return partRepository.findAll().stream()
                .map(p -> new PartResponse(p.getId(), p.getName(), p.getPrice(), p.getStockQty()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartResponse create(@Valid @RequestBody CreatePartRequest req) {
        String name = req.name().trim();
        if (partRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Part name already exists");
        }

        Part p = new Part();
        p.setName(name);
        p.setPrice(req.price());
        p.setStockQty(req.stockQty());

        partRepository.save(p);

        return new PartResponse(p.getId(), p.getName(), p.getPrice(), p.getStockQty());
    }

    @PatchMapping("/{id}/stock")
    public PartResponse updateStock(@PathVariable Long id, @Valid @RequestBody UpdateStockRequest req) {
        Part p = partRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Part not found"));

        p.setStockQty(req.stockQty());
        partRepository.save(p);

        return new PartResponse(p.getId(), p.getName(), p.getPrice(), p.getStockQty());
    }

    public record CreatePartRequest(
            @NotBlank String name,
            @NotNull @PositiveOrZero BigDecimal price,
            @NotNull @Min(0) Integer stockQty
    ) {}

    public record UpdateStockRequest(
            @NotNull @Min(0) Integer stockQty
    ) {}

    public record PartResponse(
            Long id,
            String name,
            BigDecimal price,
            int stockQty
    ) {}
}
