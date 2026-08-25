package com.garage.system.invoice;

import com.garage.system.job.Job;
import com.garage.system.job.JobPartRepository;
import com.garage.system.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final JobPartRepository jobPartRepository;
    private final NotificationService notificationService;

    public InvoiceService(InvoiceRepository invoiceRepository, JobPartRepository jobPartRepository,
                          NotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.jobPartRepository = jobPartRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Invoice createForCompletedJob(Job job) {
        return invoiceRepository.findByJob_Id(job.getId()).orElseGet(() -> createSnapshot(job));
    }

    @Transactional(readOnly = true)
    public Invoice find(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Invoice not found"));
    }

    @Transactional(readOnly = true)
    public Invoice findOwned(Long id, Long customerUserId) {
        return invoiceRepository.findByIdAndCustomerUserId(id, customerUserId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Invoice not found"));
    }

    private Invoice createSnapshot(Job job) {
        var car = job.getCar();
        var customer = car.getCustomer();
        var invoice = new Invoice();
        invoice.setJob(job);
        int year = (job.getCompletedAt() == null ? OffsetDateTime.now() : job.getCompletedAt()).getYear();
        invoice.setInvoiceNumber("INV-" + year + "-" + String.format("%06d", job.getId()));
        invoice.setCustomerUserId(customer == null || customer.getUser() == null ? null : customer.getUser().getId());
        invoice.setCustomerName(customer == null ? "Walk-in customer" : customer.getFullName());
        invoice.setCustomerEmail(customer == null ? null : customer.getEmail());
        invoice.setCustomerPhone(customer == null ? null : customer.getPhone());
        invoice.setCustomerAddress(customer == null ? null : customer.getAddress());
        invoice.setCarBrand(car.getBrand());
        invoice.setCarModel(car.getModel());
        invoice.setVin(car.getVin());
        invoice.setMechanicName(job.getMechanic().getUser().getName());
        invoice.setDescription(job.getDescription());
        invoice.setMileage(job.getMileage());
        invoice.setPartsCost(job.getPartsCost());
        invoice.setLabourCost(job.getLabourCost());
        invoice.setTotalCost(job.getTotalCost());

        for (var line : jobPartRepository.findAllByJob_Id(job.getId())) {
            var part = new InvoicePart();
            part.setPartName(line.getPart().getName());
            part.setUnitPrice(line.getUnitPrice());
            part.setQuantity(line.getQty());
            part.setLineTotal(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty())));
            invoice.addPart(part);
        }
        var saved = invoiceRepository.save(invoice);
        if (customer != null && customer.getUser() != null) {
            notificationService.create(customer.getUser(), "INVOICE_READY", "Invoice ready",
                    "Invoice " + saved.getInvoiceNumber() + " is ready for download.", "/customer/invoices");
        }
        return saved;
    }
}
