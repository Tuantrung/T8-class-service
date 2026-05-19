package com.classservice.billing;

import com.classservice.billing.dto.BillDto;
import com.classservice.billing.dto.GenerateBillsRequest;
import com.classservice.billing.dto.GenerateBillsResult;
import com.classservice.billing.dto.UpdateBillStatusRequest;
import com.classservice.common.ApiResponse;
import com.classservice.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final PdfExportService pdfExportService;

    @GetMapping
    public ResponseEntity<PageResponse<BillDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billService.listBills(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(billService.getBill(id)));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateBillsResult>> generate(@Valid @RequestBody GenerateBillsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(billService.generateBills(req)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BillDto>> updateStatus(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateBillStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(billService.updateStatus(id, req)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        BillDto bill = billService.getBill(id);
        byte[] pdf = pdfExportService.generateBillPdf(bill);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"bill-" + bill.billingMonth() + "-" + bill.studentId() + ".pdf\"")
            .body(pdf);
    }

    /**
     * Export all bills for a given month as a ZIP of PDFs.
     * Uses StreamingResponseBody to avoid buffering the entire ZIP in memory.
     */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportZip(
            @RequestParam String month) {
        // month format: YYYY-MM
        StreamingResponseBody body = outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                // TODO: filter bills by month in Track 3
                // Placeholder: iterate bills and add PDF entries
            } catch (IOException ex) {
                throw new RuntimeException("ZIP export failed", ex);
            }
        };
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bills-" + month + ".zip\"")
            .body(body);
    }
}
