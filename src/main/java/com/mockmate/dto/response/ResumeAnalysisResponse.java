package com.mockmate.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeAnalysisResponse {

    // Resume feedback
    private String overallFeedback;
    private String strengths;
    private String improvements;
    private String careerSuggestion;

    // Extracted info
    private List<String> extractedSkills;
    private String experienceLevel;
    private String suggestedRole;

    // Generated questions
    private List<String> interviewQuestions;

    // Interview ready
    private Long interviewId;
    private List<QuestionResponse> questions;
}