package com.garage.system.invoice;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/invoices")
public class CustomerInvoiceController {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;

    public CustomerInvoiceController(InvoiceRepository invoiceRepository, InvoiceService invoiceService,
                                     InvoicePdfService pdfService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
    }

    @GetMapping @Transactional(readOnly = true)
    public List<InvoiceResponse> list(Authentication auth) {
        return invoiceRepository.findAllByCustomerUserIdOrderByIssuedAtDesc((Long) auth.getPrincipal()).stream()
                .map(InvoiceResponse::from).toList();
    }

    @GetMapping("/{id}") @Transactional(readOnly = true)
    public InvoiceResponse details(Authentication auth, @PathVariable Long id) {
        return InvoiceResponse.from(invoiceService.findOwned(id, (Long) auth.getPrincipal()));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(Authentication auth, @PathVariable Long id) {
        var invoice = invoiceService.findOwned(id, (Long) auth.getPrincipal());
        return BossInvoiceController.pdfResponse(invoice, pdfService.generate(invoice));
    }
}
