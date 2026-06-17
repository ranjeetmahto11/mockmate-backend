package com.mockmate.service;

import com.mockmate.dto.request.UpdateProfileRequest;
import com.mockmate.dto.response.DashboardResponse;
import com.mockmate.dto.response.InterviewResponse;
import com.mockmate.dto.response.UserProfileResponse;
import com.mockmate.entity.*;
import com.mockmate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;


    // Get logged-in user profile

    public UserProfileResponse getMyProfile() {
        User user = getLoggedInUser();
        List<Interview> interviews = interviewRepository
                .findByUserIdOrderByStartedAtDesc(user.getId());

        // Calculate average score from completed interviews
        double avgScore = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED
                        && i.getOverallScore() != null)
                .mapToDouble(Interview::getOverallScore)
                .average()
                .orElse(0.0);

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .targetRole(user.getTargetRole())
                .experienceLevel(user.getExperienceLevel() != null
                        ? user.getExperienceLevel().name() : null)
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .totalInterviews(interviews.size())
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .build();
    }


    // Update profile

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getLoggedInUser();

        user.setFullName(request.getFullName());

        if (request.getTargetRole() != null) {
            user.setTargetRole(request.getTargetRole());
        }

        if (request.getExperienceLevel() != null) {
            user.setExperienceLevel(
                    ExperienceLevel.valueOf(
                            request.getExperienceLevel().toUpperCase()));
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());

        return getMyProfile();
    }


    // Get dashboard stats

    public DashboardResponse getDashboard() {
        User user = getLoggedInUser();

        List<Interview> allInterviews = interviewRepository
                .findByUserIdOrderByStartedAtDesc(user.getId());

        List<Interview> completed = allInterviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED
                        && i.getOverallScore() != null)
                .toList();

        List<Interview> inProgress = allInterviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.IN_PROGRESS)
                .toList();

        // Overall stats
        double avgScore = completed.stream()
                .mapToDouble(Interview::getOverallScore)
                .average().orElse(0.0);

        double highestScore = completed.stream()
                .mapToDouble(Interview::getOverallScore)
                .max().orElse(0.0);

        double lowestScore = completed.stream()
                .mapToDouble(Interview::getOverallScore)
                .min().orElse(0.0);

        // Score by category
        double technicalAvg = avgScoreByCategory(
                completed, InterviewCategory.TECHNICAL);
        double hrAvg = avgScoreByCategory(
                completed, InterviewCategory.HR);
        double behavioralAvg = avgScoreByCategory(
                completed, InterviewCategory.BEHAVIORAL);
        double systemDesignAvg = avgScoreByCategory(
                completed, InterviewCategory.SYSTEM_DESIGN);

        // Score trend — last 5 completed interviews
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy");

        List<DashboardResponse.ScoreTrendItem> scoreTrend = completed
                .stream()
                .limit(5)
                .map(i -> DashboardResponse.ScoreTrendItem.builder()
                        .interviewId(i.getId())
                        .targetRole(i.getTargetRole())
                        .category(i.getCategory().name())
                        .score(Math.round(i.getOverallScore() * 10.0) / 10.0)
                        .date(i.getCompletedAt() != null
                                ? i.getCompletedAt().format(formatter)
                                : "N/A")
                        .build())
                .toList();

        // Recent 3 interviews
        List<InterviewResponse> recentInterviews = allInterviews
                .stream()
                .limit(3)
                .map(this::mapToInterviewResponse)
                .toList();

        return DashboardResponse.builder()
                .totalInterviews(allInterviews.size())
                .completedInterviews(completed.size())
                .inProgressInterviews(inProgress.size())
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .highestScore(Math.round(highestScore * 10.0) / 10.0)
                .lowestScore(Math.round(lowestScore * 10.0) / 10.0)
                .technicalAvgScore(Math.round(technicalAvg * 10.0) / 10.0)
                .hrAvgScore(Math.round(hrAvg * 10.0) / 10.0)
                .behavioralAvgScore(Math.round(behavioralAvg * 10.0) / 10.0)
                .systemDesignAvgScore(Math.round(systemDesignAvg * 10.0) / 10.0)
                .recentInterviews(recentInterviews)
                .scoreTrend(scoreTrend)
                .build();
    }


    // Helper — avg score by category

    private double avgScoreByCategory(List<Interview> interviews,
                                      InterviewCategory category) {
        return interviews.stream()
                .filter(i -> i.getCategory() == category)
                .mapToDouble(Interview::getOverallScore)
                .average()
                .orElse(0.0);
    }


    // Helper — get logged-in user

    private User getLoggedInUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }


    // Helper — map Interview to response

    private InterviewResponse mapToInterviewResponse(Interview interview) {
        return InterviewResponse.builder()
                .interviewId(interview.getId())
                .targetRole(interview.getTargetRole())
                .category(interview.getCategory().name())
                .difficulty(interview.getDifficulty().name())
                .status(interview.getStatus().name())
                .totalQuestions(interview.getTotalQuestions())
                .overallScore(interview.getOverallScore())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .build();
    }
}