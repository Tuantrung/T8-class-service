package com.classservice.sessions.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateSessionRequest(
    LocalDate sessionDate,
    LocalTime startTime,
    LocalTime endTime,
    String topic,
    String progressNotes,
    Boolean cancelledByTeacher
) {}
