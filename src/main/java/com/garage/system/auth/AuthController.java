package com.garage.system.auth;

import com.garage.system.security.JwtService;
import com.garage.system.user.UserRepository;
import com.garage.system.mechanic.MechanicRepository;
import com.garage.system.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MechanicRepository mechanicRepository;
    private final CustomerRegistrationService registrationService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                          MechanicRepository mechanicRepository, CustomerRegistrationService registrationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mechanicRepository = mechanicRepository;
        this.registrationService = registrationService;
    }

    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerCustomer(@Valid @RequestBody CustomerRegistrationRequest req) {
        var user = registrationService.register(req.fullName(), req.phone(), req.email(), req.address(), req.password());
        return new LoginResponse(jwtService.createToken(user), user.getId(), user.getEmail(), user.getName(),
                user.getRole().name());
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getRole() == UserRole.ROLE_MECHANIC && mechanicRepository.findByUser_Id(user.getId())
                .map(mechanic -> !mechanic.isActive()).orElse(true)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Mechanic account is inactive");
        }

        String token = jwtService.createToken(user);

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        ));
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record LoginResponse(
            String token,
            Long userId,
            String email,
            String name,
            String role
    ) {}

    public record CustomerRegistrationRequest(
            @NotBlank @Size(max = 200) String fullName,
            @NotBlank @Size(max = 50) String phone,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 500) String address,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {}

}
