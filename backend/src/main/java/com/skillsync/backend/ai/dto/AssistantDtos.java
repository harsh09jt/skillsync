package com.skillsync.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AssistantDtos {

    private AssistantDtos() {}

    public record Request(
            @NotBlank @Size(max = 500) String query,
            Long jobRoleId
    ) {}

    public record SkillGap(
            long skillId,
            String skillName,
            int currentProficiency,
            int requiredProficiency,
            int gap,
            String importance
    ) {}

    public record CareerRecommendation(
            long jobRoleId,
            String roleName,
            int readinessPercentage,
            boolean ready,
            String explanation,
            List<String> skillGaps
    ) {}

    public record LearningStep(
            int order,
            String skillName,
            int currentProficiency,
            int targetProficiency,
            int gap,
            String focus,
            List<String> topics,
            String practicalProject,
            String resourceName,
            String resourceUrl,
            int estimatedWeeks
    ) {}

    public record AssessmentInsight(
            String skillName,
            int latestScore,
            String assessmentType,
            String feedback,
            String recommendation
    ) {}

    public record Response(
            String answer,
            String intent,
            String roleName,
            int readinessPercentage,
            boolean modelEnhanced,
            String modelStatus,
            List<SkillGap> skillGaps,
            List<CareerRecommendation> careerRecommendations,
            List<LearningStep> learningRoadmap,
            List<AssessmentInsight> assessmentInsights,
            List<String> dataSources
    ) {}
}
