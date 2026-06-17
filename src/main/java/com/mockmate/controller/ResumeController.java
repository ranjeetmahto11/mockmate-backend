package com.mockmate.controller;

import com.mockmate.dto.response.ResumeAnalysisResponse;
import com.mockmate.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/analyze")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @RequestParam("file") MultipartFile file) {
        try {
            ResumeAnalysisResponse response =
                    resumeService.analyzeResume(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to analyze resume: " + e.getMessage());
        }
    }
}