package com.classservice.sessions.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateSessionRequest(
    @NotNull UUID classId,
    @NotNull LocalDate sessionDate,
    LocalTime startTime,
    LocalTime endTime,
    String topic,
    Boolean cancelledByTeacher
) {}
