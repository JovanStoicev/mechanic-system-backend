package com.garage.system.auth;

import com.garage.system.customer.CustomerService;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import com.garage.system.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class CustomerRegistrationService {
    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

    public CustomerRegistrationService(UserRepository userRepository, CustomerService customerService,
                                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String fullName, String phone, String email, String address, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email already exists");
        }
        User user = new User();
        user.setName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setRole(UserRole.ROLE_CUSTOMER);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        customerService.createForUser(new CustomerService.CustomerCommand(fullName, phone, normalizedEmail, address), user);
        return user;
    }
}
