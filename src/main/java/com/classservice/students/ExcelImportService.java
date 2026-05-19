package com.classservice.students;

import com.classservice.common.TenantContext;
import com.classservice.common.exception.ExcelParseException;
import com.classservice.students.dto.ImportError;
import com.classservice.students.dto.ImportResult;
import com.classservice.students.dto.StudentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses .xlsx student roster files using Apache POI.
 * Expected columns: Full Name | Phone | Parent Phone | Notes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StudentRepository studentRepository;

    public ImportResult importStudents(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ExcelParseException("Uploaded file is empty");
        }
        List<StudentDto> imported = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            UUID tenantId = TenantContext.get();

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isBlankRow(row)) continue;

                String fullName = getCellValue(row, 0);
                if (fullName == null || fullName.isBlank()) {
                    errors.add(new ImportError(rowIdx + 1, "Full Name is required"));
                    continue;
                }

                Student student = Student.builder()
                    .tenantId(tenantId)
                    .fullName(fullName.trim())
                    .phone(getCellValue(row, 1))
                    .parentPhone(getCellValue(row, 2))
                    .notes(getCellValue(row, 3))
                    .createdAt(Instant.now())
                    .build();
                student.setTenantId(tenantId);
                studentRepository.save(student);
                imported.add(StudentDto.from(student));
            }
        } catch (IOException ex) {
            throw new ExcelParseException("Failed to read Excel file: " + ex.getMessage());
        }

        return new ImportResult(imported, errors);
    }

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] columns = {"Full Name", "Phone", "Parent Phone", "Notes"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                sheet.setColumnWidth(i, 5000);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExcelParseException("Failed to generate template: " + ex.getMessage());
        }
    }

    private String getCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> null;
        };
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getStringCellValue() != null && !cell.getStringCellValue().isBlank()) {
                return false;
            }
        }
        return true;
    }
}
