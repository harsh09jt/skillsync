package com.skillsync.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 120) String jobTitle,
        @Size(max = 120) String department,
        @Size(max = 500) String careerGoal,
        @Size(max = 1000) String bio
) {}
