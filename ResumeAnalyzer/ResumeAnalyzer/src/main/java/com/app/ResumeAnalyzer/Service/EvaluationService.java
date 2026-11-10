package com.app.ResumeAnalyzer.Service;

import com.app.ResumeAnalyzer.Entity.Evaluation;
import com.app.ResumeAnalyzer.Repository.EvaluationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository repository;

    /**
     * Check if evaluation already exists for given RFR and Candidate combination
     * Returns cached evaluation if found, null otherwise
     */
    public Evaluation getCachedEvaluation(String rfrId, String candidateId) {
        return repository.findByRfrIdAndCandidateId(rfrId, candidateId).orElse(null);
    }

    public void saveEvaluation(String rawJson, String rfrId, String candidateId) {
        try {
            // Clean the response - remove markdown code blocks
            String cleanedJson = cleanGeminiResponse(rawJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(cleanedJson);

            // Calculate scores deterministically
            JsonNode skillsNode = root.get("skills");
            double accuracyScore = calculateAccuracyScore(skillsNode);
            double complexityScore = root.has("project_complexity_score")
                ? root.get("project_complexity_score").asDouble()
                : 0.0;

            // Calculate overall fit score as weighted average
            double overallScore = (accuracyScore * 0.6) + (complexityScore * 0.4);

            // Determine status and rejection reason
            String status;
            String rejectionReason = "";

            if (accuracyScore >= 70 && complexityScore >= 60) {
                status = "SELECTED";
            } else {
                status = "REJECTED";
                List<String> reasons = new ArrayList<>();
                if (accuracyScore < 70) {
                    reasons.add("Accuracy score (" + String.format("%.1f", accuracyScore) + "%) below threshold of 70%");
                }
                if (complexityScore < 60) {
                    reasons.add("Complexity score (" + String.format("%.1f", complexityScore) + "%) below threshold of 60%");
                }
                rejectionReason = String.join("; ", reasons);
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setRfrId(rfrId);
            evaluation.setCandidateId(candidateId);
            evaluation.setOverallScore(overallScore);
            evaluation.setSkillsJson(skillsNode);
            evaluation.setAccuracyScore(accuracyScore);
            evaluation.setComplexityScore(complexityScore);
            evaluation.setStatus(status);
            evaluation.setRejectionReason(rejectionReason);

            repository.save(evaluation);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save evaluation", e);
        }
    }

    /**
     * Calculate accuracy score based on skill match, recency, and frequency
     * Formula: For each skill, calculate weighted score = (match * 0.5) + (recency * 0.3) + (frequency * 0.2)
     * Then: (sum of weighted scores / total skills / 5.0) * 100
     * This ensures recent and frequently-used skills contribute to accuracy
     */
    private double calculateAccuracyScore(JsonNode skillsNode) {
        if (skillsNode == null || skillsNode.size() == 0) {
            return 0.0;
        }

        int totalSkills = 0;
        double totalWeightedScore = 0.0;

        Iterator<Map.Entry<String, JsonNode>> fields = skillsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode skillData = entry.getValue();

            if (skillData.has("match")) {
                double matchScore = skillData.get("match").asDouble(0.0);
                double recencyScore = skillData.has("recency") ? skillData.get("recency").asDouble(0.0) : 0.0;
                double frequencyScore = skillData.has("frequency") ? skillData.get("frequency").asDouble(0.0) : 0.0;

                // Weighted formula: match (50%), recency (30%), frequency (20%)
                double weightedScore = (matchScore * 0.5) + (recencyScore * 0.3) + (frequencyScore * 0.2);
                totalWeightedScore += weightedScore;
                totalSkills++;
            }
        }

        if (totalSkills == 0) {
            return 0.0;
        }

        // Normalize to percentage (0-100)
        // Average weighted score per skill / max possible score (5.0) * 100
        return (totalWeightedScore / totalSkills / 5.0) * 100.0;
    }

    public List<Evaluation> getAllEvaluations() {
        return repository.findAll();
    }

    private String cleanGeminiResponse(String response) {
        // Remove markdown code block markers (```json and ```)
        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}
