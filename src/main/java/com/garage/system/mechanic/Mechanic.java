package com.garage.system.mechanic;

import com.garage.system.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "mechanics")
public class Mechanic {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "fixed_salary", nullable = false)
    private BigDecimal fixedSalary = BigDecimal.ZERO;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getFixedSalary() { return fixedSalary; }

    public void setUser(User user) { this.user = user; }
    public void setFixedSalary(BigDecimal fixedSalary) { this.fixedSalary = fixedSalary; }
}
