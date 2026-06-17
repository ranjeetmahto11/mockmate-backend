package com.mockmate.controller;

import com.mockmate.dto.request.InterviewStartRequest;
import com.mockmate.dto.request.SubmitAnswerRequest;
import com.mockmate.dto.response.AnswerFeedbackResponse;
import com.mockmate.dto.response.InterviewResponse;
import com.mockmate.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;

    // Start new interview
    @PostMapping("/start")
    public ResponseEntity<InterviewResponse> startInterview(
            @Valid @RequestBody InterviewStartRequest request) {
        return ResponseEntity.ok(interviewService.startInterview(request));
    }

    // Submit answer and get AI feedback
    @PostMapping("/submit-answer")
    public ResponseEntity<AnswerFeedbackResponse> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(interviewService.submitAnswer(request));
    }

    // Get my interview history
    @GetMapping("/my-interviews")
    public ResponseEntity<List<InterviewResponse>> getMyInterviews() {
        return ResponseEntity.ok(interviewService.getMyInterviews());
    }

    // Get single interview details
    @GetMapping("/{interviewId}")
    public ResponseEntity<InterviewResponse> getInterviewById(
            @PathVariable Long interviewId) {
        return ResponseEntity.ok(
                interviewService.getInterviewById(interviewId));
    }
}