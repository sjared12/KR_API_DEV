package com.example.logapi.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogEvent(
        @NotBlank @Size(max = 255) String host,
        @NotBlank @Size(max = 255) String program,
        @Min(0) @Max(7) int severity,
        @Min(0) @Max(23) int facility,
        @NotBlank @Size(max = 2048) String message
) {}
