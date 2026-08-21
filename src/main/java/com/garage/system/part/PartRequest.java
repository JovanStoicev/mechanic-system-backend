package com.garage.system.part;

import com.garage.system.mechanic.Mechanic;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "part_requests")
public class PartRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private Mechanic mechanic;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartRequestStatus status = PartRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public Mechanic getMechanic() { return mechanic; }
    public Part getPart() { return part; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }
    public PartRequestStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setMechanic(Mechanic mechanic) { this.mechanic = mechanic; }
    public void setPart(Part part) { this.part = part; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(PartRequestStatus status) { this.status = status; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
