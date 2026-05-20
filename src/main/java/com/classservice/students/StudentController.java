package com.classservice.students;

import com.classservice.common.ApiResponse;
import com.classservice.common.PageResponse;
import com.classservice.students.dto.CreateStudentRequest;
import com.classservice.students.dto.ImportResult;
import com.classservice.students.dto.StudentDto;
import com.classservice.students.dto.UpdateStudentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for student management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StudentController {

    private final StudentService studentService;
    private final ExcelImportService excelImportService;

    /**
     * List all students for the current tenant.
     */
    @GetMapping
    public ResponseEntity<PageResponse<StudentDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(studentService.listStudents(
            PageRequest.of(page, size, Sort.by("fullName").ascending())));
    }

    /**
     * Get a single student by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getStudent(id)));
    }

    /**
     * Create a student.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StudentDto>> create(@Valid @RequestBody CreateStudentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(studentService.createStudent(req), "Student created"));
    }

    /**
     * Update a student.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.updateStudent(id, req)));
    }

    /**
     * Delete a student. ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Import students from an Excel (.xlsx) file.
     * Partial success: valid rows are imported even if some rows fail (BR-011).
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResult>> importStudents(
            @RequestParam("file") MultipartFile file) {
        ImportResult result = studentService.importStudents(file);
        return ResponseEntity.ok(ApiResponse.ok(result, "Import completed"));
    }

    /**
     * Download the Excel import template.
     */
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = excelImportService.generateTemplate();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"student-import-template.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(template);
    }
}
