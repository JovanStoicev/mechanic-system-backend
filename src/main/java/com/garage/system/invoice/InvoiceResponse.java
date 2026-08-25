package com.garage.system.invoice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record InvoiceResponse(Long id, Long jobId, String invoiceNumber, String customerName,
                              String customerEmail, String customerPhone, String customerAddress,
                              String carBrand, String carModel, String vin, String mechanicName,
                              String description, int mileage, BigDecimal partsCost,
                              BigDecimal labourCost, BigDecimal totalCost, OffsetDateTime issuedAt,
                              List<PartResponse> parts) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getJob().getId(), invoice.getInvoiceNumber(),
                invoice.getCustomerName(), invoice.getCustomerEmail(), invoice.getCustomerPhone(),
                invoice.getCustomerAddress(), invoice.getCarBrand(), invoice.getCarModel(), invoice.getVin(),
                invoice.getMechanicName(), invoice.getDescription(), invoice.getMileage(),
                invoice.getPartsCost(), invoice.getLabourCost(), invoice.getTotalCost(), invoice.getIssuedAt(),
                invoice.getParts().stream().map(part -> new PartResponse(part.getPartName(),
                        part.getUnitPrice(), part.getQuantity(), part.getLineTotal())).toList());
    }

    public record PartResponse(String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
}
