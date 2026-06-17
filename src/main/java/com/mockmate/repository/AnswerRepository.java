package com.mockmate.repository;

import com.mockmate.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository
        extends JpaRepository<Answer, Long> {

    // Count answers for interview
    @Query("SELECT COUNT(a) FROM Answer a " +
            "WHERE a.question.interview.id = :interviewId")
    Long countByQuestionInterviewId(
            @Param("interviewId") Long interviewId);

    // Average score for interview
    @Query("SELECT AVG(a.score) FROM Answer a " +
            "WHERE a.question.interview.id = :interviewId")
    Double getAverageScoreByInterviewId(
            @Param("interviewId") Long interviewId);
}