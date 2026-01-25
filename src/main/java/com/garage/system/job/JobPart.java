package com.garage.system.job;

import com.garage.system.part.Part;
import jakarta.persistence.*;

@Entity
@Table(name = "job_parts")
public class JobPart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private int qty;

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public Part getPart() { return part; }
    public int getQty() { return qty; }

    public void setJob(Job job) { this.job = job; }
    public void setPart(Part part) { this.part = part; }
    public void setQty(int qty) { this.qty = qty; }
}
