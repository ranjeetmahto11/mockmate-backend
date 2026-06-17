package com.mockmate.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerFeedbackResponse {
    private Long answerId;
    private Integer score;           // out of 10
    private String aiFeedback;       // overall feedback
    private String strengths;        // what was good
    private String improvements;     // what to improve
    private boolean isLastQuestion;
    private Double overallScore;     // only set when last question
}