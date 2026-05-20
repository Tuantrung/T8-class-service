package com.classservice.students;

import com.classservice.common.TenantContext;
import com.classservice.students.dto.ImportResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExcelImportService.importStudents().
 * No Spring context — StudentRepository is mocked with Mockito.
 * Test Excel files are built in-memory using Apache POI.
 *
 * Scenarios:
 *   1. Valid file with 3 student rows  → imported=3, errors empty
 *   2. 1 valid row + 1 missing-name row → imported=1, errors has 1 entry with row number
 *   3. Header-only file (empty)        → imported=0, no errors
 */
@ExtendWith(MockitoExtension.class)
class ExcelImportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ExcelImportService excelImportService;

    @BeforeEach
    void setUp() {
        TenantContext.set(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // -----------------------------------------------------------------------
    // Scenario 1: Valid file with 3 data rows
    // -----------------------------------------------------------------------

    @Test
    void parseExcel_validFileWithThreeRows_importsAllThreeStudents() throws IOException {
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile file = buildExcel(wb -> {
            Sheet sheet = wb.createSheet("Students");
            createHeaderRow(sheet);
            createDataRow(sheet, 1, "Nguyen Van A", "0901234567", "0912345678", "Gioi");
            createDataRow(sheet, 2, "Tran Thi B",  "0902345678", null,         null);
            createDataRow(sheet, 3, "Le Van C",    null,         null,         "Can co gang");
        });

        ImportResult result = excelImportService.importStudents(file);

        assertThat(result.imported()).isEqualTo(3);
        assertThat(result.skipped()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(studentRepository, times(3)).save(any(Student.class));
    }

    // -----------------------------------------------------------------------
    // Scenario 2: Mixed valid + invalid (missing required Full Name)
    // -----------------------------------------------------------------------

    @Test
    void parseExcel_oneValidAndOneMissingName_importsOneAndReportsOneError() throws IOException {
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile file = buildExcel(wb -> {
            Sheet sheet = wb.createSheet("Students");
            createHeaderRow(sheet);
            createDataRow(sheet, 1, "Nguyen Van A", "0901234567", null, null); // valid
            createDataRow(sheet, 2, null,            "0912345678", null, null); // missing Full Name
        });

        ImportResult result = excelImportService.importStudents(file);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);

        // Row number is 1-indexed and skips the header; data row 2 is Excel row 3
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(3);
        assertThat(result.errors().get(0).message()).containsIgnoringCase("Full Name");
    }

    // -----------------------------------------------------------------------
    // Scenario 3: Header-only file (empty — no data rows)
    // -----------------------------------------------------------------------

    @Test
    void parseExcel_headerOnlyFile_importsZeroAndHasNoErrors() throws IOException {
        MultipartFile file = buildExcel(wb -> {
            Sheet sheet = wb.createSheet("Students");
            createHeaderRow(sheet);
            // No data rows added
        });

        ImportResult result = excelImportService.importStudents(file);

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(studentRepository, times(0)).save(any(Student.class));
    }

    // -----------------------------------------------------------------------
    // Scenario 4: Blank rows between data rows are skipped
    // -----------------------------------------------------------------------

    @Test
    void parseExcel_withInterspersedBlankRows_skipsThemAndCountsCorrectly() throws IOException {
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile file = buildExcel(wb -> {
            Sheet sheet = wb.createSheet("Students");
            createHeaderRow(sheet);
            createDataRow(sheet, 1, "Nguyen Van A", "0901234567", null, null);
            // Row index 2 is blank (not created)
            createDataRow(sheet, 3, "Tran Thi B", "0902345678", null, null);
        });

        ImportResult result = excelImportService.importStudents(file);

        // The blank row at index 2 is skipped (row == null)
        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Scenario 5: Numeric phone values in cells are handled
    // -----------------------------------------------------------------------

    @Test
    void parseExcel_numericPhoneCells_areConvertedToString() throws IOException {
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile file = buildExcel(wb -> {
            Sheet sheet = wb.createSheet("Students");
            createHeaderRow(sheet);

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Dinh Quoc D");
            row.createCell(1).setCellValue(901234567.0);  // numeric phone
        });

        ImportResult result = excelImportService.importStudents(file);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Builder helpers
    // -----------------------------------------------------------------------

    @FunctionalInterface
    interface WorkbookConfigurator {
        void configure(Workbook wb);
    }

    private MultipartFile buildExcel(WorkbookConfigurator configurator) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            configurator.configure(wb);
            wb.write(out);
            byte[] bytes = out.toByteArray();
            return new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
            );
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Full Name");
        header.createCell(1).setCellValue("Phone");
        header.createCell(2).setCellValue("Parent Phone");
        header.createCell(3).setCellValue("Notes");
    }

    private void createDataRow(Sheet sheet, int rowIndex, String fullName,
                               String phone, String parentPhone, String notes) {
        Row row = sheet.createRow(rowIndex);
        if (fullName != null)     row.createCell(0).setCellValue(fullName);
        if (phone != null)        row.createCell(1).setCellValue(phone);
        if (parentPhone != null)  row.createCell(2).setCellValue(parentPhone);
        if (notes != null)        row.createCell(3).setCellValue(notes);
    }
}
