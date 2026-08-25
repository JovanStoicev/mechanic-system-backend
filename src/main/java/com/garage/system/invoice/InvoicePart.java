package com.garage.system.invoice;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_parts")
public class InvoicePart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    @Column(name = "part_name", nullable = false) private String partName;
    @Column(name = "unit_price", nullable = false) private BigDecimal unitPrice;
    @Column(nullable = false) private int quantity;
    @Column(name = "line_total", nullable = false) private BigDecimal lineTotal;

    public Long getId() { return id; }
    public String getPartName() { return partName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    void setInvoice(Invoice invoice) { this.invoice = invoice; }
    void setPartName(String partName) { this.partName = partName; }
    void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    void setQuantity(int quantity) { this.quantity = quantity; }
    void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
