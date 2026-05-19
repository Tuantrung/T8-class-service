package com.classservice.auth;

import com.classservice.auth.dto.ChangePasswordRequest;
import com.classservice.auth.dto.CreateUserRequest;
import com.classservice.auth.dto.LoginRequest;
import com.classservice.auth.dto.LoginResponse;
import com.classservice.auth.dto.RegisterTenantRequest;
import com.classservice.auth.dto.UserDto;
import com.classservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-tenant")
    public ResponseEntity<LoginResponse> registerTenant(@Valid @RequestBody RegisterTenantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTenant(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(ApiResponse.ok(UserDto.from(user)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CreateUserRequest req) {
        UserDto created = authService.createUser(currentUser.getTenantId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> listUsers(@AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(authService.listUsers(currentUser.getTenantId())));
    }
}
