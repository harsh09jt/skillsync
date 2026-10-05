package com.skillsync.backend.ai.agent;

import com.skillsync.backend.ai.dto.AssistantDtos.AssessmentInsight;
import com.skillsync.backend.ai.dto.AssistantDtos.CareerRecommendation;
import com.skillsync.backend.ai.dto.AssistantDtos.LearningStep;
import com.skillsync.backend.ai.dto.AssistantDtos.Request;
import com.skillsync.backend.ai.dto.AssistantDtos.Response;
import com.skillsync.backend.ai.dto.AssistantDtos.SkillGap;
import com.skillsync.backend.ai.service.OpenAiCompatibleClient;
import com.skillsync.backend.ai.tools.SkillSyncAgentTools;
import com.skillsync.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SupervisorAgent {

    private static final Pattern INTENT_PATTERN =
            Pattern.compile("\\b(SKILL_GAP|CAREER|LEARNING|ASSESSMENT|ALL)\\b");

    private final SkillGapAgent skillGapAgent;
    private final AssessmentAgent assessmentAgent;
    private final CareerAgent careerAgent;
    private final LearningAgent learningAgent;
    private final SkillSyncAgentTools tools;
    private final OpenAiCompatibleClient aiClient;

    public SupervisorAgent(
            SkillGapAgent skillGapAgent,
            AssessmentAgent assessmentAgent,
            CareerAgent careerAgent,
            LearningAgent learningAgent,
            SkillSyncAgentTools tools,
            OpenAiCompatibleClient aiClient
    ) {
        this.skillGapAgent = skillGapAgent;
        this.assessmentAgent = assessmentAgent;
        this.careerAgent = careerAgent;
        this.learningAgent = learningAgent;
        this.tools = tools;
        this.aiClient = aiClient;
    }

    public Response handle(User user, Request request) {
        String query = request.query().trim();
        Long roleId = tools.findRoleId(user, query, request.jobRoleId());
        String roleName = roleId == null ? "" : tools.roleName(roleId);
        String intent = classifyIntent(query);

        List<SkillGap> gaps = skillGapAgent.analyze(user, roleId);
        List<AssessmentInsight> assessments = assessmentAgent.analyze(user);
        List<CareerRecommendation> careers = careerAgent.recommend(user, roleId);
        int readiness = careers.stream()
                .filter(career -> roleId != null && career.jobRoleId() == roleId)
                .mapToInt(CareerRecommendation::readinessPercentage)
                .findFirst()
                .orElse(0);
        LearningAgent.RoadmapResult roadmapResult = learningAgent.roadmap(gaps, roleName);
        List<LearningStep> roadmap = roadmapResult.steps();

        String answer = answer(roleName, gaps, readiness, careers, assessments, intent);
        boolean modelEnhanced = aiClient.isConfigured();
        String modelStatus = modelEnhanced
                ? "Request intent uses " + aiClient.model() + "; "
                        + (roadmapResult.modelGenerated()
                                ? "learning topics were generated for your recorded gaps"
                                : "local learning guidance is shown")
                        + ". Skill levels and readiness are calculated from your account data."
                : "Gemini is not configured. Add GEMINI_API_KEY to backend/.env; data-backed analysis is active.";

        return new Response(
                answer,
                intent,
                roleName,
                readiness,
                modelEnhanced,
                modelStatus,
                gaps,
                careers,
                roadmap,
                assessments,
                List.of(
                        "Your employee skill profile",
                        "Configured job-role requirements",
                        "Your recorded assessment results"
                )
        );
    }

    private String classifyIntent(String query) {
        String classification = aiClient.complete(
                "You are a request router. Output one allowed label only. Do not provide facts.",
                """
                Classify this request as exactly one label: SKILL_GAP, CAREER, LEARNING, ASSESSMENT, or ALL.
                Return only the label.
                Request: %s
                """.formatted(query)
        ).orElse("");
        Matcher matcher = INTENT_PATTERN.matcher(classification.toUpperCase());
        return matcher.find() ? matcher.group(1) : inferIntent(query);
    }

    private String inferIntent(String query) {
        String text = query.toLowerCase();
        if (text.contains("assessment") || text.contains("score") || text.contains("weak topic")) return "ASSESSMENT";
        if (text.contains("roadmap") || text.contains("learn") || text.contains("study")) return "LEARNING";
        if (text.contains("career") || text.contains("recommend") || text.contains("suitable role")) return "CAREER";
        if (text.contains("gap") || text.contains("missing skill")) return "SKILL_GAP";
        return "ALL";
    }

    private String answer(
            String roleName,
            List<SkillGap> gaps,
            int readiness,
            List<CareerRecommendation> careers,
            List<AssessmentInsight> assessments,
            String intent
    ) {
        if (!roleName.isBlank()) {
            if (gaps.isEmpty()) {
                return "I couldn't find configured required skills for " + roleName
                        + ". Ask your workspace admin to configure this role's skill requirements before readiness can be calculated.";
            }
            List<String> priorities = gaps.stream()
                    .filter(gap -> gap.gap() > 0)
                    .limit(3)
                    .map(SkillGap::skillName)
                    .toList();
            String readinessLine = "Your recorded profile meets " + readiness
                    + "% of the configured " + roleName + " proficiency requirements.";
            String gapLine = priorities.isEmpty()
                    ? "Your recorded proficiency meets the configured targets."
                    : "Prioritize " + String.join(", ", priorities) + " to close the largest recorded gaps.";
            return readinessLine + " " + gapLine + roadmapSentence(gaps);
        }
        if (careers.isEmpty()) {
            return "No job roles have required skills configured yet, so I can't calculate career readiness. Ask an admin to add role requirements.";
        }
        CareerRecommendation best = careers.getFirst();
        return switch (intent) {
            case "ASSESSMENT" -> assessments.isEmpty()
                    ? "There are no recorded assessments yet. Complete a skill assessment to receive score-based recommendations."
                    : "I reviewed " + assessments.size() + " latest recorded skill assessment(s). "
                    + assessments.stream().filter(item -> item.latestScore() < 6).map(item -> item.skillName() + " (" + item.latestScore() + "/10)").limit(3).reduce((a, b) -> a + ", " + b).map(weak -> "Focus on " + weak + ".").orElse("Your latest recorded scores are at least 6/10.");
            case "LEARNING" -> "Your strongest configured role match is " + best.roleName() + " at "
                    + best.readinessPercentage() + "% readiness." + roadmapSentence(gaps);
            case "CAREER" -> "Based on recorded skills and configured requirements, your closest role match is "
                    + best.roleName() + " at " + best.readinessPercentage() + "% readiness. " + best.explanation();
            case "SKILL_GAP" -> "Your closest role match is " + best.roleName() + ". "
                    + best.explanation();
            default -> "I reviewed your recorded skills, assessment history and configured roles. Your closest match is "
                    + best.roleName() + " at " + best.readinessPercentage() + "% readiness. "
                    + best.explanation() + roadmapSentence(gaps);
        };
    }

    private String roadmapSentence(List<SkillGap> gaps) {
        List<String> next = gaps.stream()
                .filter(gap -> gap.gap() > 0)
                .limit(3)
                .map(SkillGap::skillName)
                .toList();
        return next.isEmpty() ? "" : " Suggested learning order: " + String.join(" → ", next) + ".";
    }

}
