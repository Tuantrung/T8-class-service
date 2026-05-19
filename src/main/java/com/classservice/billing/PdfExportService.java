package com.classservice.billing;

import com.classservice.billing.dto.BillDto;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Generates bill PDFs using iText 7 Community.
 * Generates in-memory (no file system) and returns byte[].
 */
@Slf4j
@Service
public class PdfExportService {

    /**
     * Generate a PDF for a single bill.
     * TODO: Replace placeholder content with full bill layout in Track 3.
     */
    public byte[] generateBillPdf(BillDto bill) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("BILL").setBold().setFontSize(18));
            document.add(new Paragraph("Student ID: " + bill.studentId()));
            document.add(new Paragraph("Class ID:   " + bill.classId()));
            document.add(new Paragraph("Month:      " + bill.billingMonth()));
            document.add(new Paragraph(""));

            // Session summary table
            Table table = new Table(3);
            table.addHeaderCell("Sessions Total");
            table.addHeaderCell("Sessions Attended");
            table.addHeaderCell("Rate / Session");
            table.addCell(String.valueOf(bill.sessionsTotal()));
            table.addCell(String.valueOf(bill.sessionsAttended()));
            table.addCell(bill.ratePerSession().toString());
            document.add(table);

            document.add(new Paragraph(""));
            document.add(new Paragraph("TOTAL: " + bill.totalAmount()).setBold().setFontSize(14));
            document.add(new Paragraph("Status: " + bill.status()));

        } catch (Exception ex) {
            log.error("Failed to generate PDF for bill {}", bill.id(), ex);
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
        return out.toByteArray();
    }
}
