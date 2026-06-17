package com.mockmate.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    private Integer totalInterviews;
    private Integer completedInterviews;
    private Integer inProgressInterviews;
    private Double averageScore;
    private Double highestScore;
    private Double lowestScore;

    // Score by category
    private Double technicalAvgScore;
    private Double hrAvgScore;
    private Double behavioralAvgScore;
    private Double systemDesignAvgScore;

    // Recent interviews
    private List<InterviewResponse> recentInterviews;

    // Score trend (last 5 interviews)
    private List<ScoreTrendItem> scoreTrend;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScoreTrendItem {
        private Long interviewId;
        private String targetRole;
        private String category;
        private Double score;
        private String date;
    }
}