package com.classservice.classes;

import com.classservice.classes.dto.ClassDto;
import com.classservice.classes.dto.CreateClassRequest;
import com.classservice.classes.dto.EnrollStudentsRequest;
import com.classservice.classes.dto.UpdateClassRequest;
import com.classservice.common.ApiResponse;
import com.classservice.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<PageResponse<ClassDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(classService.listClasses(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(classService.getClass(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClassDto>> create(@Valid @RequestBody CreateClassRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(classService.createClass(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> update(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateClassRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(classService.updateClass(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/students")
    public ResponseEntity<Void> enrollStudents(@PathVariable UUID id,
                                                @Valid @RequestBody EnrollStudentsRequest req) {
        classService.enrollStudents(id, req.studentIds());
        return ResponseEntity.noContent().build();
    }
}
