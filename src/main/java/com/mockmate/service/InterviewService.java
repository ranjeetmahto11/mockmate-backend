package com.mockmate.service;

import com.mockmate.dto.request.InterviewStartRequest;
import com.mockmate.dto.request.SubmitAnswerRequest;
import com.mockmate.dto.response.AnswerFeedbackResponse;
import com.mockmate.dto.response.InterviewResponse;
import com.mockmate.dto.response.QuestionResponse;
import com.mockmate.entity.*;
import com.mockmate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final GroqAiService groqAiService;

    // Start a new interview session

    @Transactional
    public InterviewResponse startInterview(InterviewStartRequest request) {

        // Get logged-in user
        User user = getLoggedInUser();

        // Generate questions via GroqAi
        log.info("Generating {} questions for role: {}",
                request.getTotalQuestions(), request.getTargetRole());

        List<String> generatedQuestions = groqAiService.generateQuestions(
                request.getTargetRole(),
                request.getCategory(),
                request.getDifficulty(),
                request.getTotalQuestions()
        );

        // Create interview
        Interview interview = Interview.builder()
                .user(user)
                .targetRole(request.getTargetRole())
                .category(InterviewCategory.valueOf(
                        request.getCategory().toUpperCase()))
                .difficulty(DifficultyLevel.valueOf(
                        request.getDifficulty().toUpperCase()))
                .status(InterviewStatus.IN_PROGRESS)
                .totalQuestions(request.getTotalQuestions())
                .build();

        interview = interviewRepository.save(interview);

        // Save questions
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < generatedQuestions.size(); i++) {
            Question question = Question.builder()
                    .interview(interview)
                    .questionText(generatedQuestions.get(i))
                    .questionOrder(i + 1)
                    .build();
            questions.add(questionRepository.save(question));
        }

        // Build response
        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> QuestionResponse.builder()
                        .questionId(q.getId())
                        .questionText(q.getQuestionText())
                        .questionOrder(q.getQuestionOrder())
                        .totalQuestions(request.getTotalQuestions())
                        .build())
                .toList();

        return InterviewResponse.builder()
                .interviewId(interview.getId())
                .targetRole(interview.getTargetRole())
                .category(interview.getCategory().name())
                .difficulty(interview.getDifficulty().name())
                .status(interview.getStatus().name())
                .totalQuestions(interview.getTotalQuestions())
                .startedAt(interview.getStartedAt())
                .questions(questionResponses)
                .build();
    }

    // ─────────────────────────────────────────
    // Submit answer and get AI feedback
    // ─────────────────────────────────────────
    @Transactional
    public AnswerFeedbackResponse submitAnswer(
            SubmitAnswerRequest request) {

        // Get interview and question
        Interview interview = interviewRepository
                .findById(request.getInterviewId())
                .orElseThrow(() ->
                        new RuntimeException("Interview not found"));

        Question question = questionRepository
                .findById(request.getQuestionId())
                .orElseThrow(() ->
                        new RuntimeException("Question not found"));

        // Get AI evaluation
        log.info("Evaluating answer for question: {}",
                question.getQuestionText());

        Map<String, String> evaluation = groqAiService.evaluateAnswer(
                question.getQuestionText(),
                request.getAnswerText(),
                interview.getTargetRole()
        );

        // Save answer
        Answer answer = Answer.builder()
                .question(question)
                .answerText(request.getAnswerText())
                .score(Integer.parseInt(evaluation.getOrDefault("score", "5")))
                .aiFeedback(evaluation.getOrDefault("feedback", ""))
                .strengths(evaluation.getOrDefault("strengths", ""))
                .improvements(evaluation.getOrDefault("improvements", ""))
                .build();

        answerRepository.save(answer);

        answerRepository.flush();

        // Check if this was the last question
      //  List<Question> allQuestions = questionRepository
        //        .findByInterviewIdOrderByQuestionOrder(interview.getId());

        Long answeredCount = answerRepository
                .countByQuestionInterviewId(interview.getId());

        log.info("Answered: {} / Total: {}",
                answeredCount, interview.getTotalQuestions());

        // ── Fix: check if last question ──
        boolean isLastQuestion =
                answeredCount >= interview.getTotalQuestions();

        Double overallScore = null;

        if (isLastQuestion) {
            // Get average score directly from DB
            Double avgScore = answerRepository
                    .getAverageScoreByInterviewId(interview.getId());

            overallScore = avgScore != null
                    ? Math.round(avgScore * 10.0) / 10.0
                    : 0.0;

            log.info("Overall score: {}", overallScore);

            interview.setOverallScore(overallScore);
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setCompletedAt(LocalDateTime.now());
            interviewRepository.save(interview);

            log.info("Interview {} marked COMPLETED",
                    interview.getId());
        }

        return AnswerFeedbackResponse.builder()
                .answerId(answer.getId())
                .score(answer.getScore())
                .aiFeedback(answer.getAiFeedback())
                .strengths(answer.getStrengths())
                .improvements(answer.getImprovements())
                .isLastQuestion(isLastQuestion)
                .overallScore(overallScore)
                .build();
    }


    // Get interview history for logged-in user

    public List<InterviewResponse> getMyInterviews() {
        User user = getLoggedInUser();
        List<Interview> interviews = interviewRepository
                .findByUserIdOrderByStartedAtDesc(user.getId());

        return interviews.stream()
                .map(this::mapToInterviewResponse)
                .toList();
    }


    // Get single interview details

    public InterviewResponse getInterviewById(Long interviewId) {
        Interview interview = interviewRepository
                .findByIdWithQuestionsAndAnswers(interviewId)
                .orElseThrow(() ->
                        new RuntimeException("Interview not found"));
        return mapToInterviewResponse(interview);
    }


    // Helper — calculate overall score

    private Double calculateOverallScore(Long interviewId) {
        List<Question> questions = questionRepository
                .findByInterviewIdOrderByQuestionOrder(interviewId);

        return questions.stream()
                .filter(q -> q.getAnswer() != null)
                .mapToInt(q -> q.getAnswer().getScore())
                .average()
                .orElse(0.0);
    }


    // Helper — get logged-in user

    private User getLoggedInUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    // Helper — map Interview entity to response

    private InterviewResponse mapToInterviewResponse(Interview interview) {
        List<QuestionResponse> questionResponses = new ArrayList<>();

        if (interview.getQuestions() != null) {
            questionResponses = interview.getQuestions().stream()
                    .sorted(Comparator.comparing(
                            Question::getQuestionOrder))
                    .map(q -> {
                        // Include answer if exists
                        AnswerFeedbackResponse answerResponse = null;
                        if (q.getAnswer() != null) {
                            Answer a = q.getAnswer();
                            answerResponse = AnswerFeedbackResponse.builder()
                                    .answerId(a.getId())
                                    .score(a.getScore())
                                    .aiFeedback(a.getAiFeedback())
                                    .strengths(a.getStrengths())
                                    .improvements(a.getImprovements())
                                    .build();
                        }
                        return QuestionResponse.builder()
                                .questionId(q.getId())
                                .questionText(q.getQuestionText())
                                .questionOrder(q.getQuestionOrder())
                                .totalQuestions(interview.getTotalQuestions())
                                .answer(answerResponse)
                                .build();
                    })
                    .toList();
        }

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
                .questions(questionResponses)
                .build();
    }
}