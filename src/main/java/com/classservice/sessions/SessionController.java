package com.classservice.sessions;

import com.classservice.common.ApiResponse;
import com.classservice.sessions.dto.CreateSessionRequest;
import com.classservice.sessions.dto.SessionDto;
import com.classservice.sessions.dto.UpdateSessionRequest;
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
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionDto>>> listByClass(@RequestParam UUID classId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.listByClass(classId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSession(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SessionDto>> create(@Valid @RequestBody CreateSessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(sessionService.createSession(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> update(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.updateSession(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
