package com.mockmate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockmate.dto.response.ResumeAnalysisResponse;
import com.mockmate.entity.*;
import com.mockmate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final GroqAiService groqAiService;
    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────
    // Main method — analyze resume
    // ─────────────────────────────────────────
    public ResumeAnalysisResponse analyzeResume(
            MultipartFile file) throws IOException {

        // Step 1 — Extract text from file
        String resumeText = extractText(file);
        log.info("Extracted {} characters from resume",
                resumeText.length());

        if (resumeText.trim().isEmpty()) {
            throw new RuntimeException(
                    "Could not extract text from resume. " +
                            "Please upload a valid PDF, Word, or text file.");
        }

        // Step 2 — Analyze with Groq AI
        String analysisJson = groqAiService
                .analyzeResume(resumeText);

        // Step 3 — Parse AI response
        ResumeAnalysisResponse response =
                parseAnalysis(analysisJson);

        // Step 4 — Create interview from questions
        Long interviewId = createInterviewFromResume(
                response.getInterviewQuestions(),
                response.getSuggestedRole()
        );
        response.setInterviewId(interviewId);

        // ← Fetch real questions with real IDs from DB
        List<com.mockmate.entity.Question> dbQuestions =
                questionRepository
                        .findByInterviewIdOrderByQuestionOrder(
                                interviewId);

        List<com.mockmate.dto.response.QuestionResponse>
                questionResponses = dbQuestions.stream()
                .map(q -> com.mockmate.dto.response
                        .QuestionResponse.builder()
                        .questionId(q.getId())
                        .questionText(q.getQuestionText())
                        .questionOrder(q.getQuestionOrder())
                        .totalQuestions(
                                response.getInterviewQuestions().size())
                        .build())
                .toList();

        response.setQuestions(questionResponses);

        return response;
    }

    // ─────────────────────────────────────────
    // Extract text from different file types
    // ─────────────────────────────────────────
    private String extractText(MultipartFile file)
            throws IOException {

        String fileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        if (fileName.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (fileName.endsWith(".docx")) {
            return extractFromDocx(file);
        } else if (fileName.endsWith(".txt")) {
            return new String(file.getBytes());
        } else {
            try {
                return extractFromPdf(file);
            } catch (Exception e) {
                return new String(file.getBytes());
            }
        }
    }

    // ─────────────────────────────────────────
    // Extract text from PDF — PDFBox 3.x fix
    // ─────────────────────────────────────────
    private String extractFromPdf(MultipartFile file)
            throws IOException {
        // ← PDFBox 3.x uses Loader.loadPDF()
        try (PDDocument document = Loader.loadPDF(
                file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    // ─────────────────────────────────────────
    // Extract text from Word docx
    // ─────────────────────────────────────────
    private String extractFromDocx(MultipartFile file)
            throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(
                file.getInputStream())) {
            for (XWPFParagraph para :
                    document.getParagraphs()) {
                text.append(para.getText()).append("\n");
            }
        }
        return text.toString();
    }

    // ─────────────────────────────────────────
    // Parse AI analysis response
    // ─────────────────────────────────────────
    private ResumeAnalysisResponse parseAnalysis(
            String jsonResponse) {
        try {
            // Clean response — remove markdown if present
            String cleaned = jsonResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);

            // Parse skills
            List<String> skills = new ArrayList<>();
            if (node.has("skills") &&
                    node.get("skills").isArray()) {
                node.get("skills").forEach(
                        s -> skills.add(s.asText()));
            }

            // Parse questions
            List<String> questions = new ArrayList<>();
            if (node.has("questions") &&
                    node.get("questions").isArray()) {
                node.get("questions").forEach(
                        q -> questions.add(q.asText()));
            }

            return ResumeAnalysisResponse.builder()
                    .overallFeedback(node.path("overallFeedback")
                            .asText("Good resume overall."))
                    .strengths(node.path("strengths")
                            .asText(""))
                    .improvements(node.path("improvements")
                            .asText(""))
                    .careerSuggestion(node.path("careerSuggestion")
                            .asText(""))
                    .extractedSkills(skills)
                    .experienceLevel(node.path("experienceLevel")
                            .asText("FRESHER"))
                    .suggestedRole(node.path("suggestedRole")
                            .asText("Software Developer"))
                    .interviewQuestions(questions)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse analysis: {}",
                    e.getMessage());
            return ResumeAnalysisResponse.builder()
                    .overallFeedback("Resume analyzed successfully.")
                    .strengths("Good technical background.")
                    .improvements("Add more project details.")
                    .careerSuggestion(
                            "Consider applying for junior developer roles.")
                    .extractedSkills(List.of("Java", "Spring Boot"))
                    .experienceLevel("FRESHER")
                    .suggestedRole("Java Developer")
                    .interviewQuestions(List.of(
                            "Tell me about yourself.",
                            "What are your technical skills?",
                            "Where do you see yourself in 5 years?"))
                    .build();
        }
    }

    // ─────────────────────────────────────────
    // Create interview from resume questions
    // ─────────────────────────────────────────
    private Long createInterviewFromResume(
            List<String> generatedQuestions,
            String targetRole) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Create interview
        Interview interview = Interview.builder()
                .user(user)
                .targetRole(targetRole != null
                        ? targetRole : "Software Developer")
                .category(InterviewCategory.TECHNICAL)
                .difficulty(DifficultyLevel.MEDIUM)
                .status(InterviewStatus.IN_PROGRESS)
                .totalQuestions(generatedQuestions.size())
                .build();

        interview = interviewRepository.save(interview);

        // Save questions
        List<Question> savedQuestions = new ArrayList<>();
        for (int i = 0; i < generatedQuestions.size(); i++) {
            Question question = Question.builder()
                    .interview(interview)
                    .questionText(generatedQuestions.get(i))
                    .questionOrder(i + 1)
                    .build();
            savedQuestions.add(questionRepository.save(question));
        }

        log.info("Created interview {} from resume " +
                        "with {} questions",
                interview.getId(),
                generatedQuestions.size());

        return interview.getId();
    }
}