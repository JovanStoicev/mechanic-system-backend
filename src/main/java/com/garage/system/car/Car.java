package com.garage.system.car;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cars")
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String brand;
    @Column(nullable = false) private String model;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name="engine_type", nullable = false)
    private String engineType;

    @Column(name="horse_power", nullable = false)
    private int horsePower;

    @Column(name="created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getVin() { return vin; }
    public String getEngineType() { return engineType; }
    public int getHorsePower() { return horsePower; }

    public void setBrand(String brand) { this.brand = brand; }
    public void setModel(String model) { this.model = model; }
    public void setVin(String vin) { this.vin = vin; }
    public void setEngineType(String engineType) { this.engineType = engineType; }
    public void setHorsePower(int horsePower) { this.horsePower = horsePower; }
}
