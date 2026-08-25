package com.garage.system.invoice;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {
    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color LIGHT = new Color(241, 245, 249);

    public byte[] generate(Invoice invoice) {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(document, output);
            document.addTitle("Invoice " + invoice.getInvoiceNumber());
            document.addAuthor("Garage System");
            document.open();

            Font title = new Font(Font.HELVETICA, 24, Font.BOLD, NAVY);
            Font heading = new Font(Font.HELVETICA, 11, Font.BOLD, NAVY);
            Font body = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font white = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

            document.add(new Paragraph("GARAGE SYSTEM", title));
            var subtitle = new Paragraph("SERVICE INVOICE", new Font(Font.HELVETICA, 12, Font.BOLD, Color.GRAY));
            subtitle.setSpacingAfter(22);
            document.add(subtitle);

            PdfPTable header = new PdfPTable(new float[]{1, 1});
            header.setWidthPercentage(100);
            header.addCell(infoCell("Invoice number", invoice.getInvoiceNumber(), heading, body));
            header.addCell(infoCell("Issued", invoice.getIssuedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), heading, body));
            header.addCell(infoCell("Customer", invoice.getCustomerName(), heading, body));
            header.addCell(infoCell("Mechanic", invoice.getMechanicName(), heading, body));
            header.addCell(infoCell("Vehicle", invoice.getCarBrand() + " " + invoice.getCarModel(), heading, body));
            header.addCell(infoCell("VIN / mileage", invoice.getVin() + " / " + invoice.getMileage() + " km", heading, body));
            header.setSpacingAfter(20);
            document.add(header);

            var workHeading = new Paragraph("Work performed", heading);
            workHeading.setSpacingAfter(6);
            document.add(workHeading);
            var description = new Paragraph(invoice.getDescription(), body);
            description.setSpacingAfter(18);
            document.add(description);

            PdfPTable parts = new PdfPTable(new float[]{4, 1, 1.5f, 1.5f});
            parts.setWidthPercentage(100);
            for (String label : new String[]{"Part", "Qty", "Unit price", "Line total"}) {
                var cell = new PdfPCell(new Phrase(label, white));
                cell.setBackgroundColor(NAVY); cell.setPadding(8); parts.addCell(cell);
            }
            if (invoice.getParts().isEmpty()) {
                var cell = new PdfPCell(new Phrase("No parts used", body));
                cell.setColspan(4); cell.setPadding(9); parts.addCell(cell);
            } else {
                for (var part : invoice.getParts()) {
                    parts.addCell(tableCell(part.getPartName(), body));
                    parts.addCell(tableCell(String.valueOf(part.getQuantity()), body));
                    parts.addCell(tableCell(money(part.getUnitPrice()), body));
                    parts.addCell(tableCell(money(part.getLineTotal()), body));
                }
            }
            parts.setSpacingAfter(16);
            document.add(parts);

            PdfPTable totals = new PdfPTable(new float[]{3, 1.5f});
            totals.setWidthPercentage(52); totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.addCell(totalCell("Parts", body, false)); totals.addCell(totalCell(money(invoice.getPartsCost()), body, false));
            totals.addCell(totalCell("Mechanic work", body, false)); totals.addCell(totalCell(money(invoice.getLabourCost()), body, false));
            totals.addCell(totalCell("TOTAL", heading, true)); totals.addCell(totalCell(money(invoice.getTotalCost()), heading, true));
            totals.setSpacingAfter(26);
            document.add(totals);

            var note = new Paragraph("Thank you for choosing Garage System.", new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY));
            note.setAlignment(Element.ALIGN_CENTER);
            document.add(note);
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Could not generate invoice PDF", exception);
        }
    }

    private PdfPCell infoCell(String label, String value, Font heading, Font body) {
        var cell = new PdfPCell(); cell.setBorder(Rectangle.NO_BORDER); cell.setBackgroundColor(LIGHT); cell.setPadding(10);
        cell.addElement(new Phrase(label, heading));
        cell.addElement(new Phrase(value == null || value.isBlank() ? "-" : value, body));
        return cell;
    }

    private PdfPCell tableCell(String value, Font font) { var cell = new PdfPCell(new Phrase(value, font)); cell.setPadding(8); return cell; }
    private PdfPCell totalCell(String value, Font font, boolean emphasized) { var cell = tableCell(value, font); if (emphasized) cell.setBackgroundColor(LIGHT); return cell; }
    private String money(BigDecimal amount) { return "EUR " + amount.setScale(2).toPlainString(); }
}
