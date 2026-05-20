package com.classservice.auth.dto;

public record LoginResponse(
    String accessToken,
    UserDto user
) {}
