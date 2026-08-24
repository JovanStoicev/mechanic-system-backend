package com.garage.system.customer;

import com.garage.system.car.CarRepository;
import com.garage.system.user.User;
import com.garage.system.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, CarRepository carRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Customer create(CustomerCommand command) {
        var customer = new Customer();
        apply(customer, command);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer createForUser(CustomerCommand command, User user) {
        var customer = new Customer();
        customer.setUser(user);
        apply(customer, command);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerCommand command) {
        var customer = find(id);
        apply(customer, command);
        customer.setUpdatedAt(OffsetDateTime.now());
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        var customer = find(id);
        for (var car : carRepository.findAllByCustomer_IdOrderByBrandAscModelAsc(id)) {
            car.setCustomer(null);
        }
        var user = customer.getUser();
        customerRepository.delete(customer);
        customerRepository.flush();
        if (user != null) userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
    }

    private void apply(Customer customer, CustomerCommand command) {
        String phone = command.phone().trim();
        String email = normalizeNullable(command.email(), true);
        customerRepository.findByPhone(phone)
                .filter(existing -> !existing.getId().equals(customer.getId()))
                .ifPresent(existing -> { throw new ResponseStatusException(CONFLICT, "Phone already exists"); });
        if (email != null) {
            customerRepository.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getId().equals(customer.getId()))
                    .ifPresent(existing -> { throw new ResponseStatusException(CONFLICT, "Email already exists"); });
        }

        customer.setFullName(command.fullName().trim());
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(normalizeNullable(command.address(), false));
    }

    private String normalizeNullable(String value, boolean lowercase) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return lowercase ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    public record CustomerCommand(String fullName, String phone, String email, String address) {}
}
