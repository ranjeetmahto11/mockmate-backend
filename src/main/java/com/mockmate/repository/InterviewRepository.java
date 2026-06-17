// InterviewRepository.java
package com.mockmate.repository;

import com.mockmate.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserIdOrderByStartedAtDesc(Long userId);

    // Eagerly load questions + answers
    @Query("SELECT i FROM Interview i " +
            "LEFT JOIN FETCH i.questions q " +
            "LEFT JOIN FETCH q.answer " +
            "WHERE i.id = :id")
    Optional<Interview> findByIdWithQuestionsAndAnswers(
            @Param("id") Long id);
}