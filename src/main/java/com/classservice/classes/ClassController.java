package com.classservice.classes;

import com.classservice.classes.dto.ClassDto;
import com.classservice.classes.dto.CreateClassRequest;
import com.classservice.classes.dto.EnrollResult;
import com.classservice.classes.dto.EnrollStudentsRequest;
import com.classservice.classes.dto.UpdateClassRequest;
import com.classservice.common.ApiResponse;
import com.classservice.common.PageResponse;
import com.classservice.students.dto.StudentDto;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for tutoring class management.
 * All endpoints require authentication; some require ADMIN role.
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ClassController {

    private final ClassService classService;

    /**
     * List all classes for the current tenant, with pagination.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ClassDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(classService.listClasses(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /**
     * Get a single class by ID.
     */
    @GetMapping("/{classId}")
    public ResponseEntity<ApiResponse<ClassDto>> get(@PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.ok(classService.getClass(classId)));
    }

    /**
     * Create a new class. Requires ADMIN or TEACHER role.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ClassDto>> create(@Valid @RequestBody CreateClassRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(classService.createClass(req), "Class created"));
    }

    /**
     * Update an existing class.
     */
    @PutMapping("/{classId}")
    public ResponseEntity<ApiResponse<ClassDto>> update(
            @PathVariable UUID classId,
            @Valid @RequestBody UpdateClassRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(classService.updateClass(classId, req)));
    }

    /**
     * Archive (soft-delete) a class. ADMIN only.
     */
    @DeleteMapping("/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID classId) {
        classService.deactivateClass(classId);
        return ResponseEntity.noContent().build();
    }

    /**
     * List students enrolled in a class.
     */
    @GetMapping("/{classId}/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> listStudents(@PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.ok(classService.listStudentsInClass(classId)));
    }

    /**
     * Enroll one or more students in a class.
     */
    @PostMapping("/{classId}/students")
    public ResponseEntity<ApiResponse<EnrollResult>> enrollStudents(
            @PathVariable UUID classId,
            @Valid @RequestBody EnrollStudentsRequest req) {
        EnrollResult result = classService.enrollStudents(classId, req.studentIds());
        return ResponseEntity.ok(ApiResponse.ok(result, "Enrollment processed"));
    }

    /**
     * Remove a student from a class.
     */
    @DeleteMapping("/{classId}/students/{studentId}")
    public ResponseEntity<Void> removeStudent(
            @PathVariable UUID classId,
            @PathVariable UUID studentId) {
        classService.removeStudent(classId, studentId);
        return ResponseEntity.noContent().build();
    }
}
