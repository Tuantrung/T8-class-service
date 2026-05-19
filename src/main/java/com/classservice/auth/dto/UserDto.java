package com.classservice.auth.dto;

import com.classservice.auth.AppUser;
import com.classservice.auth.UserRole;

import java.util.UUID;

public record UserDto(
    UUID id,
    UUID tenantId,
    String email,
    String fullName,
    UserRole role
) {
    public static UserDto from(AppUser user) {
        return new UserDto(user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
