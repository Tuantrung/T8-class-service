package com.classservice.grades;

import com.classservice.common.ApiResponse;
import com.classservice.grades.dto.CreateGradeRequest;
import com.classservice.grades.dto.GradeDto;
import com.classservice.grades.dto.UpdateGradeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GradeDto>>> list(
            @RequestParam UUID classId,
            @RequestParam(required = false) UUID studentId) {
        List<GradeDto> grades = studentId != null
            ? gradeService.listByClassAndStudent(classId, studentId)
            : gradeService.listByClass(classId);
        return ResponseEntity.ok(ApiResponse.ok(grades));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GradeDto>> create(@Valid @RequestBody CreateGradeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(gradeService.createGrade(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GradeDto>> update(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateGradeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(gradeService.updateGrade(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.noContent().build();
    }
}
