package com.classservice.students;

import com.classservice.classes.ClassStudent;
import com.classservice.classes.ClassStudentRepository;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.ExcelParseException;
import com.classservice.students.dto.ImportError;
import com.classservice.students.dto.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses .xlsx student roster files using Apache POI.
 * Expected columns (0-indexed): Full Name | Phone | Parent Phone | Notes
 * Partial success: valid rows are saved even if some rows fail (BR-011).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StudentRepository studentRepository;
    private final ClassStudentRepository classStudentRepository;

    /**
     * Parse and import students from an .xlsx file.
     * Row 0 is assumed to be a header row and is skipped.
     * Returns ImportResult with count of imported students, skipped blank rows,
     * and a list of row-level errors for rows that failed validation.
     *
     * @param file the uploaded .xlsx file
     * @return ImportResult with counts and errors
     * @throws ExcelParseException if the file cannot be read
     */
    @Transactional
    public ImportResult importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("Uploaded file is empty");
        }

        int imported = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();

        UUID tenantId = TenantContext.get();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);

                // Skip blank rows
                if (row == null || isBlankRow(row)) {
                    skipped++;
                    continue;
                }

                // Col 0: Full Name (required)
                String fullName = getCellValue(row, 0);
                if (fullName == null || fullName.isBlank()) {
                    errors.add(new ImportError(rowIdx + 1, "Full Name is required"));
                    continue;
                }

                String phone = getCellValue(row, 1);
                String parentPhone = getCellValue(row, 2);
                String notes = getCellValue(row, 3);

                Student student = Student.builder()
                    .tenantId(tenantId)
                    .fullName(fullName.trim())
                    .phone(phone)
                    .parentPhone(parentPhone)
                    .notes(notes)
                    .createdAt(Instant.now())
                    .build();
                studentRepository.save(student);
                imported++;
            }
        } catch (IOException ex) {
            log.error("Failed to read Excel file for tenant {}: {}", tenantId, ex.getMessage());
            throw new ExcelParseException("Failed to read Excel file: " + ex.getMessage());
        }

        log.info("Excel import complete: imported={}, skipped={}, errors={}", imported, skipped, errors.size());
        return new ImportResult(imported, skipped, errors);
    }

    /**
     * Import students from an Excel file and immediately enroll them into the given class.
     * Students that are already enrolled are silently skipped (no duplicate entry).
     */
    @Transactional
    public ImportResult importStudents(MultipartFile file, UUID classId) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("Uploaded file is empty");
        }

        int imported = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();
        UUID tenantId = TenantContext.get();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);

                if (row == null || isBlankRow(row)) {
                    skipped++;
                    continue;
                }

                String fullName = getCellValue(row, 0);
                if (fullName == null || fullName.isBlank()) {
                    errors.add(new ImportError(rowIdx + 1, "Full Name is required"));
                    continue;
                }

                String phone = getCellValue(row, 1);
                String parentPhone = getCellValue(row, 2);
                String notes = getCellValue(row, 3);

                Student student = Student.builder()
                    .tenantId(tenantId)
                    .fullName(fullName.trim())
                    .phone(phone)
                    .parentPhone(parentPhone)
                    .notes(notes)
                    .createdAt(Instant.now())
                    .build();
                Student saved = studentRepository.save(student);

                // Enroll into the class (skip if already enrolled)
                ClassStudent.ClassStudentId csId = new ClassStudent.ClassStudentId(classId, saved.getId());
                if (!classStudentRepository.existsById(csId)) {
                    classStudentRepository.save(
                        ClassStudent.builder().id(csId).enrolledAt(Instant.now()).build());
                }

                imported++;
            }
        } catch (IOException ex) {
            log.error("Failed to read Excel file for tenant {}: {}", tenantId, ex.getMessage());
            throw new ExcelParseException("Failed to read Excel file: " + ex.getMessage());
        }

        log.info("Excel import+enroll for class {}: imported={}, skipped={}, errors={}",
            classId, imported, skipped, errors.size());
        return new ImportResult(imported, skipped, errors);
    }

    /**
     * Generate an empty .xlsx template with header row only.
     * Columns: Full Name | Phone | Parent Phone | Notes
     *
     * @return byte array of the generated .xlsx file
     */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] columns = {"Full Name", "Phone", "Parent Phone", "Notes"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                sheet.setColumnWidth(i, 5000);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to generate Excel template: {}", ex.getMessage());
            throw new ExcelParseException("Failed to generate template: " + ex.getMessage());
        }
    }

    /**
     * Extract the string value from a cell, supporting STRING and NUMERIC types.
     *
     * @param row the row to read from
     * @param col the column index (0-based)
     * @return string value or null if cell is empty or unsupported type
     */
    private String getCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String v = cell.getStringCellValue();
                yield (v == null || v.isBlank()) ? null : v.trim();
            }
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    /**
     * Determine if all cells in the first four columns are blank.
     *
     * @param row the row to check
     * @return true if the row is blank
     */
    private boolean isBlankRow(Row row) {
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;
            if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isBlank()) return false;
            if (cell.getCellType() == CellType.NUMERIC) return false;
        }
        return true;
    }
}
