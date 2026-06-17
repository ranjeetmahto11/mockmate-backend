package com.mockmate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewStartRequest {

    @NotBlank(message = "Target role is required")
    private String targetRole;        // e.g. "Java Developer"

    @NotBlank(message = "Category is required")
    private String category;          // TECHNICAL, HR, BEHAVIORAL, SYSTEM_DESIGN

    @NotBlank(message = "Difficulty is required")
    private String difficulty;        // EASY, MEDIUM, HARD

    @NotNull(message = "Number of questions is required")
    private Integer totalQuestions;   // 5 or 10
}