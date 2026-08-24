package com.garage.system.customer;

import com.garage.system.mechanic.MechanicRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/mechanics")
public class CustomerMechanicController {
    private final MechanicRepository mechanicRepository;
    public CustomerMechanicController(MechanicRepository mechanicRepository) { this.mechanicRepository = mechanicRepository; }

    @GetMapping
    public List<MechanicOption> list() {
        return mechanicRepository.findAll().stream().filter(mechanic -> mechanic.isActive())
                .map(mechanic -> new MechanicOption(mechanic.getId(), mechanic.getUser().getName()))
                .toList();
    }

    public record MechanicOption(Long id, String name) {}
}
