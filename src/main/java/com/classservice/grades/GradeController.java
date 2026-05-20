package com.classservice.grades;

import com.classservice.common.ApiResponse;
import com.classservice.common.PageResponse;
import com.classservice.grades.dto.CreateGradeRequest;
import com.classservice.grades.dto.GradeDto;
import com.classservice.grades.dto.UpdateGradeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

/**
 * REST controller for grade management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GradeController {

    private final GradeService gradeService;

    /**
     * List grades for a class, optionally filtered by student.
     */
    @GetMapping
    public ResponseEntity<PageResponse<GradeDto>> list(
            @RequestParam UUID classId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gradeService.listGrades(
            classId, studentId,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /**
     * Create a grade entry.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GradeDto>> create(@Valid @RequestBody CreateGradeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(gradeService.createGrade(req), "Grade created"));
    }

    /**
     * Update a grade entry.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GradeDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGradeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(gradeService.updateGrade(id, req)));
    }

    /**
     * Delete a grade entry.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.noContent().build();
    }
}
