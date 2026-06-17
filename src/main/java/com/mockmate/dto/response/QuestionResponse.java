package com.mockmate.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResponse {
    private Long questionId;
    private String questionText;
    private Integer questionOrder;
    private Integer totalQuestions;
    private AnswerFeedbackResponse answer;
}