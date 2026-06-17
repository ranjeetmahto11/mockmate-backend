package com.mockmate.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String targetRole;
    private String experienceLevel;
    private String role;
    private LocalDateTime createdAt;
    private Integer totalInterviews;
    private Double averageScore;
}