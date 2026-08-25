package com.garage.system.invoice;

import com.garage.system.job.Job;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;
    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;
    @Column(name = "customer_user_id") private Long customerUserId;
    @Column(name = "customer_name", nullable = false) private String customerName;
    @Column(name = "customer_email") private String customerEmail;
    @Column(name = "customer_phone") private String customerPhone;
    @Column(name = "customer_address") private String customerAddress;
    @Column(name = "car_brand", nullable = false) private String carBrand;
    @Column(name = "car_model", nullable = false) private String carModel;
    @Column(nullable = false, length = 17) private String vin;
    @Column(name = "mechanic_name", nullable = false) private String mechanicName;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(nullable = false) private int mileage;
    @Column(name = "parts_cost", nullable = false) private BigDecimal partsCost;
    @Column(name = "labour_cost", nullable = false) private BigDecimal labourCost;
    @Column(name = "total_cost", nullable = false) private BigDecimal totalCost;
    @Column(name = "issued_at", nullable = false) private OffsetDateTime issuedAt = OffsetDateTime.now();
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InvoicePart> parts = new ArrayList<>();

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Long getCustomerUserId() { return customerUserId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerAddress() { return customerAddress; }
    public String getCarBrand() { return carBrand; }
    public String getCarModel() { return carModel; }
    public String getVin() { return vin; }
    public String getMechanicName() { return mechanicName; }
    public String getDescription() { return description; }
    public int getMileage() { return mileage; }
    public BigDecimal getPartsCost() { return partsCost; }
    public BigDecimal getLabourCost() { return labourCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public List<InvoicePart> getParts() { return parts; }

    void setJob(Job job) { this.job = job; }
    void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    void setCustomerUserId(Long customerUserId) { this.customerUserId = customerUserId; }
    void setCustomerName(String customerName) { this.customerName = customerName; }
    void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    void setCarBrand(String carBrand) { this.carBrand = carBrand; }
    void setCarModel(String carModel) { this.carModel = carModel; }
    void setVin(String vin) { this.vin = vin; }
    void setMechanicName(String mechanicName) { this.mechanicName = mechanicName; }
    void setDescription(String description) { this.description = description; }
    void setMileage(int mileage) { this.mileage = mileage; }
    void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    void setLabourCost(BigDecimal labourCost) { this.labourCost = labourCost; }
    void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    void addPart(InvoicePart part) { parts.add(part); part.setInvoice(this); }
}
