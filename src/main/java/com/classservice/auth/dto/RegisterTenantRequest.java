package com.classservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
    @NotBlank String centerName,
    @Email @NotBlank String adminEmail,
    @NotBlank @Size(min = 8) String adminPassword,
    @NotBlank String adminFullName
) {}
