package com.mockmate.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewResponse {
    private Long interviewId;
    private String targetRole;
    private String category;
    private String difficulty;
    private String status;
    private Integer totalQuestions;
    private Double overallScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<QuestionResponse> questions;
}