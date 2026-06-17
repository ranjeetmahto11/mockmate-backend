package com.mockmate.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String targetRole;        // e.g. "Java Developer"
    private String experienceLevel;   // FRESHER, JUNIOR, MID, SENIOR
}