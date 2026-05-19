package com.classservice.auth;

import com.classservice.auth.dto.ChangePasswordRequest;
import com.classservice.auth.dto.CreateUserRequest;
import com.classservice.auth.dto.LoginRequest;
import com.classservice.auth.dto.LoginResponse;
import com.classservice.auth.dto.RegisterTenantRequest;
import com.classservice.auth.dto.UserDto;
import com.classservice.common.exception.BadCredentialsException;
import com.classservice.common.exception.DuplicateResourceException;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.tenant.Tenant;
import com.classservice.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse registerTenant(RegisterTenantRequest req) {
        Tenant tenant = Tenant.builder()
            .name(req.centerName())
            .createdAt(Instant.now())
            .build();
        tenantRepository.save(tenant);

        if (userRepository.findByTenantIdAndEmail(tenant.getId(), req.adminEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already registered: " + req.adminEmail());
        }

        AppUser admin = AppUser.builder()
            .tenantId(tenant.getId())
            .email(req.adminEmail())
            .passwordHash(passwordEncoder.encode(req.adminPassword()))
            .fullName(req.adminFullName())
            .role(UserRole.ADMIN)
            .createdAt(Instant.now())
            .build();
        userRepository.save(admin);

        String token = jwtUtil.generateToken(admin.getId(), admin.getTenantId(), admin.getRole());
        return new LoginResponse(token, UserDto.from(admin));
    }

    public LoginResponse login(LoginRequest req) {
        AppUser user = userRepository.findByTenantIdAndEmail(req.tenantId(), req.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getTenantId(), user.getRole());
        return new LoginResponse(token, UserDto.from(user));
    }

    @Transactional
    public UserDto createUser(UUID tenantId, CreateUserRequest req) {
        if (userRepository.findByTenantIdAndEmail(tenantId, req.email()).isPresent()) {
            throw new DuplicateResourceException("Email already registered: " + req.email());
        }
        AppUser user = AppUser.builder()
            .tenantId(tenantId)
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .fullName(req.fullName())
            .role(req.role())
            .createdAt(Instant.now())
            .build();
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("AppUser", userId));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public List<UserDto> listUsers(UUID tenantId) {
        // TODO: add findAllByTenantId query to UserRepository
        return userRepository.findAll().stream()
            .filter(u -> tenantId.equals(u.getTenantId()))
            .map(UserDto::from)
            .toList();
    }
}
