package com.classservice.billing;

import com.classservice.billing.dto.BillDetailDto;
import com.classservice.billing.dto.BillDto;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
public class PdfExportService {

    private static final Locale VN = new Locale("vi", "VN");

    public byte[] generateBillPdf(BillDetailDto bill) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header
            doc.add(new Paragraph("PHIEU HOC PHI")
                .setFont(bold).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Thang " + formatMonth(bill.billingMonth()))
                .setFont(bold).setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" "));

            // Info table
            Table info = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth();
            addInfoRow(info, "Hoc sinh:", bill.studentName(), bold, regular);
            addInfoRow(info, "Lop hoc:", bill.className(), bold, regular);
            addInfoRow(info, "Ky tinh phi:", "Thang " + formatMonth(bill.billingMonth()), bold, regular);
            doc.add(info);
            doc.add(new Paragraph(" "));

            // Session table
            doc.add(new Paragraph("Chi tiet buoi hoc:").setFont(bold).setFontSize(11));
            Table sessions = new Table(UnitValue.createPercentArray(new float[]{8, 18, 32, 18, 24}))
                .useAllAvailableWidth();
            addHeaderCell(sessions, "STT", bold);
            addHeaderCell(sessions, "Ngay hoc", bold);
            addHeaderCell(sessions, "Buoi hoc", bold);
            addHeaderCell(sessions, "Co mat", bold);
            addHeaderCell(sessions, "Ghi chu", bold);

            int idx = 1;
            for (BillDetailDto.SessionLineItem s : bill.sessions()) {
                sessions.addCell(new Cell().add(new Paragraph(String.valueOf(idx++)).setFont(regular).setFontSize(9)));
                sessions.addCell(new Cell().add(new Paragraph(s.sessionDate().toString()).setFont(regular).setFontSize(9)));
                sessions.addCell(new Cell().add(new Paragraph(s.topic() != null ? s.topic() : "").setFont(regular).setFontSize(9)));
                String present = s.cancelledByTeacher() ? "-" : (s.attended() ? "Co" : "Vang");
                sessions.addCell(new Cell().add(new Paragraph(present).setFont(regular).setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)));
                String note = s.cancelledByTeacher() ? "GV huy" : "";
                sessions.addCell(new Cell().add(new Paragraph(note).setFont(regular).setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)));
            }
            doc.add(sessions);
            doc.add(new Paragraph(" "));

            // Summary
            NumberFormat vnd = NumberFormat.getNumberInstance(VN);
            Table summary = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
            addSummaryRow(summary, "So buoi co mat:", bill.sessionsAttended() + " buoi", regular, bold);
            addSummaryRow(summary, "Hoc phi / buoi:", vnd.format(bill.ratePerSession()) + " VND", regular, bold);
            doc.add(summary);

            doc.add(new Paragraph("TONG HOC PHI: " + vnd.format(bill.totalAmount()) + " VND")
                .setFont(bold).setFontSize(14).setTextAlignment(TextAlignment.RIGHT));

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Xin cam on quy phu huynh da tin tuong!")
                .setFont(regular).setFontSize(10).setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        } catch (Exception ex) {
            log.error("Failed to generate PDF for bill {}", bill.id(), ex);
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
        return out.toByteArray();
    }

    public byte[] generateBillPdf(BillDto bill) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            NumberFormat vnd = NumberFormat.getNumberInstance(VN);

            doc.add(new Paragraph("PHIEU HOC PHI").setFont(bold).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("So buoi co mat: " + bill.sessionsAttended() + " buoi").setFont(regular));
            doc.add(new Paragraph("Hoc phi / buoi: " + vnd.format(bill.ratePerSession()) + " VND").setFont(regular));
            doc.add(new Paragraph("TONG: " + vnd.format(bill.totalAmount()) + " VND").setFont(bold).setFontSize(14));

        } catch (Exception ex) {
            log.error("Failed to generate PDF for bill {}", bill.id(), ex);
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
        return out.toByteArray();
    }

    private String formatMonth(String billingMonth) {
        String[] parts = billingMonth.split("-");
        return parts[1] + "/" + parts[0];
    }

    private void addInfoRow(Table t, String label, String value, PdfFont bold, PdfFont regular) {
        t.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        t.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }

    private void addHeaderCell(Table t, String text, PdfFont bold) {
        t.addHeaderCell(new Cell().add(new Paragraph(text).setFont(bold).setFontSize(9))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
    }

    private void addSummaryRow(Table t, String label, String value, PdfFont regular, PdfFont bold) {
        t.addCell(new Cell().add(new Paragraph(label).setFont(regular).setFontSize(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        t.addCell(new Cell().add(new Paragraph(value).setFont(bold).setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }
}
