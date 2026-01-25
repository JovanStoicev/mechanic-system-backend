package com.garage.system.mechanic;

import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;

@RestController
@RequestMapping("/api/boss/mechanics")
public class BossMechanicController {

    private final UserRepository userRepository;
    private final MechanicRepository mechanicRepository;
    private final PasswordEncoder passwordEncoder;

    public BossMechanicController(
            UserRepository userRepository,
            MechanicRepository mechanicRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.mechanicRepository = mechanicRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<MechanicResponse> list() {
        return mechanicRepository.findAll().stream()
                .map(m -> new MechanicResponse(
                        m.getId(),
                        m.getUser().getName(),
                        m.getUser().getEmail(),
                        m.getFixedSalary()
                ))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public MechanicResponse create(@Valid @RequestBody CreateMechanicRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email already exists");
        }

        User u = new User();
        u.setEmail(email);
        u.setName(req.name().trim());
        u.setRole(UserRole.ROLE_MECHANIC);
        u.setPasswordHash(passwordEncoder.encode(req.tempPassword()));

        userRepository.save(u);

        Mechanic m = new Mechanic();
        m.setUser(u);
        m.setFixedSalary(req.fixedSalary());

        mechanicRepository.save(m);

        return new MechanicResponse(
                m.getId(),
                u.getName(),
                u.getEmail(),
                m.getFixedSalary()
        );
    }

    public record CreateMechanicRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotNull @PositiveOrZero BigDecimal fixedSalary,
            @NotBlank String tempPassword
    ) {}

    public record MechanicResponse(
            Long id,
            String name,
            String email,
            BigDecimal fixedSalary
    ) {}
}
