package com.garage.system.invoice;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boss/invoices")
public class BossInvoiceController {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;

    public BossInvoiceController(InvoiceRepository invoiceRepository, InvoiceService invoiceService,
                                 InvoicePdfService pdfService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
    }

    @GetMapping @Transactional(readOnly = true)
    public List<InvoiceResponse> list() {
        return invoiceRepository.findAllByOrderByIssuedAtDesc().stream().map(InvoiceResponse::from).toList();
    }

    @GetMapping("/{id}") @Transactional(readOnly = true)
    public InvoiceResponse details(@PathVariable Long id) { return InvoiceResponse.from(invoiceService.find(id)); }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var invoice = invoiceService.find(id);
        return pdfResponse(invoice, pdfService.generate(invoice));
    }

    static ResponseEntity<byte[]> pdfResponse(Invoice invoice, byte[] bytes) {
        var disposition = ContentDisposition.attachment()
                .filename(invoice.getInvoiceNumber() + ".pdf").build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF).contentLength(bytes.length).body(bytes);
    }
}
