package com.app.ResumeAnalyzer.Repository;

import com.app.ResumeAnalyzer.Entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByRfrIdAndCandidateId(String rfrId, String candidateId);
}
