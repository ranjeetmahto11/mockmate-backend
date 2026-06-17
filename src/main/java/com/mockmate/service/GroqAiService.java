package com.mockmate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAiService {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    // Generate interview questions using GPT

    public List<String> generateQuestions(String targetRole,
                                          String category,
                                          String difficulty,
                                          int totalQuestions) {
        String prompt = buildQuestionPrompt(
                targetRole, category, difficulty, totalQuestions);

        String response = callOpenAi(prompt);

        return parseQuestions(response, totalQuestions);
    }


    // Evaluate answer using GPT

    public Map<String, String> evaluateAnswer(String question,
                                              String answer,
                                              String targetRole) {
        String prompt = buildEvaluationPrompt(question, answer, targetRole);
        String response = callOpenAi(prompt);
        return parseEvaluation(response);
    }


    // Call OpenAI API

    private String callOpenAi(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "system",
                                    "content", "You are an expert technical interviewer."),
                            Map.of("role", "user",
                                    "content", prompt)
                    ),
                    "max_tokens", 1000,
                    "temperature", 0.7
            );

            String responseJson = openAiRestClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service error: " + e.getMessage());
        }
    }


    // Build question generation prompt

    private String buildQuestionPrompt(String role,
                                       String category,
                                       String difficulty,
                                       int total) {
        return String.format("""
            You are an expert interviewer for %s roles.
            Generate exactly %d %s interview questions for a %s level candidate.
            Category: %s
            
            Rules:
            - Return ONLY a numbered list like: 1. question
            - No extra text, no explanations, just the questions
            - Each question on a new line
            - Questions should be relevant and practical
            
            Example format:
            1. What is the difference between JDK, JRE and JVM?
            2. Explain SOLID principles with examples.
            """,
                role, total, difficulty, difficulty, category);
    }


    // Build answer evaluation prompt

    private String buildEvaluationPrompt(String question,
                                         String answer,
                                         String role) {
        return String.format("""
            You are evaluating a candidate interview answer for a %s role.
            
            Question: %s
            Candidate Answer: %s
            
            Evaluate the answer and respond in EXACTLY this JSON format:
            {
                "score": <number from 1 to 10>,
                "feedback": "<overall feedback in 2-3 sentences>",
                "strengths": "<what the candidate did well>",
                "improvements": "<what the candidate should improve>"
            }
            
            Return ONLY the JSON. No extra text.
            """,
                role, question, answer);
    }


    // Parse GPT questions response

    private List<String> parseQuestions(String response, int total) {
        try {
            return response.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> line.matches("^\\d+\\..*"))
                    .map(line -> line.replaceFirst("^\\d+\\.\\s*", ""))
                    .limit(total)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse questions: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI questions");
        }
    }


    // Parse GPT evaluation response

    private Map<String, String> parseEvaluation(String response) {
        try {
            // Clean response — remove markdown code blocks if present
            String cleaned = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);

            return Map.of(
                    "score",        node.path("score").asText("5"),
                    "feedback",     node.path("feedback").asText(""),
                    "strengths",    node.path("strengths").asText(""),
                    "improvements", node.path("improvements").asText("")
            );
        } catch (Exception e) {
            log.error("Failed to parse evaluation: {}", e.getMessage());
            // Return default feedback if parsing fails
            return Map.of(
                    "score",        "5",
                    "feedback",     "Answer received and evaluated.",
                    "strengths",    "You attempted the question.",
                    "improvements", "Try to be more specific and detailed."
            );
        }
    }
    // ─────────────────────────────────────────
// Analyze resume text with Groq AI
// ─────────────────────────────────────────
    public String analyzeResume(String resumeText) {
        String prompt = String.format("""
        You are an expert HR consultant and technical interviewer.
        Analyze this resume and respond in EXACTLY this JSON format:

        {
            "overallFeedback": "<2-3 sentence overall assessment>",
            "strengths": "<key strengths of the candidate>",
            "improvements": "<what the candidate should improve>",
            "careerSuggestion": "<career path suggestion>",
            "experienceLevel": "<FRESHER or JUNIOR or MID or SENIOR>",
            "suggestedRole": "<best matching job role>",
            "skills": ["skill1", "skill2", "skill3"],
            "questions": [
                "question1 based on resume",
                "question2 based on resume",
                "question3 based on resume",
                "question4 based on resume",
                "question5 based on resume"
            ]
        }

        Rules:
        - Return ONLY valid JSON, no extra text
        - No markdown backticks
        - Generate exactly 5 interview questions
        - Questions must be specific to candidate's skills
        - Skills array should have 5-10 items

        Resume:
        %s
        """, resumeText);

        // ← This calls Groq AI automatically!
        return callOpenAi(prompt);
    }

}