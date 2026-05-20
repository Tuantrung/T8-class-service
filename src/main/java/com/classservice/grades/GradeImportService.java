package com.classservice.grades;

import com.classservice.classes.ClassStudentRepository;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.ExcelParseException;
import com.classservice.grades.dto.GradeImportError;
import com.classservice.grades.dto.GradeImportResult;
import com.classservice.students.Student;
import com.classservice.students.StudentRepository;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeImportService {

    private final GradeRepository gradeRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;

    /**
     * Import grades from an Excel file. Columns: [Họ tên] [Điểm]
     * Row 0 is the header and is skipped.
     * Matches students by full name (case-insensitive) within the enrolled class.
     * Creates a new grade if none exists for the (classId, studentId, examName) combo;
     * otherwise updates the existing score.
     */
    @Transactional
    public GradeImportResult importGrades(UUID classId, String examName, LocalDate examDate,
                                           MultipartFile file) {
        UUID tenantId = TenantContext.get();

        // Build a name → student map from enrolled students
        List<UUID> enrolledIds = classStudentRepository.findAllByIdClassId(classId)
            .stream()
            .map(cs -> cs.getId().getStudentId())
            .toList();

        Map<String, Student> nameToStudent = studentRepository.findAllById(enrolledIds)
            .stream()
            .collect(Collectors.toMap(
                s -> s.getFullName().trim().toLowerCase(),
                Function.identity(),
                (a, b) -> a  // keep first on duplicate name
            ));

        int imported = 0;
        int updated = 0;
        int skipped = 0;
        List<GradeImportError> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isBlankRow(row)) continue;

                String nameRaw = getCellString(row, 0);
                String scoreRaw = getCellString(row, 1);

                if (nameRaw == null || nameRaw.isBlank()) {
                    skipped++;
                    continue;
                }

                Student student = nameToStudent.get(nameRaw.trim().toLowerCase());
                if (student == null) {
                    errors.add(new GradeImportError(rowIdx + 1,
                        "Không tìm thấy học sinh \"" + nameRaw + "\" trong lớp"));
                    continue;
                }

                BigDecimal score = null;
                if (scoreRaw != null && !scoreRaw.isBlank()) {
                    try {
                        score = new BigDecimal(scoreRaw.trim().replace(",", "."));
                    } catch (NumberFormatException e) {
                        errors.add(new GradeImportError(rowIdx + 1,
                            "Điểm không hợp lệ cho học sinh \"" + nameRaw + "\": " + scoreRaw));
                        continue;
                    }
                }

                Optional<Grade> existing = gradeRepository.findByClassIdAndStudentIdAndExamName(
                    classId, student.getId(), examName);

                if (existing.isPresent()) {
                    Grade g = existing.get();
                    g.setScore(score);
                    if (examDate != null) g.setExamDate(examDate);
                    gradeRepository.save(g);
                    updated++;
                } else {
                    Grade g = Grade.builder()
                        .tenantId(tenantId)
                        .classId(classId)
                        .studentId(student.getId())
                        .examName(examName)
                        .examDate(examDate)
                        .score(score)
                        .createdAt(Instant.now())
                        .build();
                    gradeRepository.save(g);
                    imported++;
                }
            }
        } catch (IOException ex) {
            log.error("Failed to read grade import file for tenant {}: {}", tenantId, ex.getMessage());
            throw new ExcelParseException("Failed to read Excel file: " + ex.getMessage());
        }

        log.info("Grade import for class {} exam '{}': {} imported, {} updated, {} skipped, {} errors",
            classId, examName, imported, updated, skipped, errors.size());
        return new GradeImportResult(imported, updated, skipped, errors);
    }

    /**
     * Generate a downloadable Excel template for grade import.
     * Columns: Họ tên | Điểm
     */
    public byte[] generateTemplate(UUID classId) {
        List<UUID> enrolledIds = classStudentRepository.findAllByIdClassId(classId)
            .stream()
            .map(cs -> cs.getId().getStudentId())
            .toList();

        List<Student> students = studentRepository.findAllById(enrolledIds);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Điểm");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Họ tên");
            header.createCell(1).setCellValue("Điểm");

            int rowIdx = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getFullName());
                row.createCell(1).setCellValue("");
            }

            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 3000);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExcelParseException("Failed to generate grade template: " + ex.getMessage());
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            default -> null;
        };
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < 2; i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;
            if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isBlank()) return false;
            if (cell.getCellType() == CellType.NUMERIC) return false;
        }
        return true;
    }
}
