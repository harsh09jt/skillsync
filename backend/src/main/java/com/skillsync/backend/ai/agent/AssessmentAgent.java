package com.skillsync.backend.ai.agent;

import com.skillsync.backend.ai.dto.AssistantDtos.AssessmentInsight;
import com.skillsync.backend.ai.tools.SkillSyncAgentTools;
import com.skillsync.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AssessmentAgent {

    private final SkillSyncAgentTools tools;

    public AssessmentAgent(SkillSyncAgentTools tools) {
        this.tools = tools;
    }

    public List<AssessmentInsight> analyze(User user) {
        Map<Long, Map<String, Object>> latestBySkill = new LinkedHashMap<>();
        tools.assessments(user).forEach(assessment ->
                latestBySkill.putIfAbsent(number(assessment.get("skillId")), assessment)
        );
        return latestBySkill.values().stream()
                .map(assessment -> {
                    int score = (int) number(assessment.get("score"));
                    String skill = String.valueOf(assessment.get("skillName"));
                    return new AssessmentInsight(
                            skill,
                            score,
                            String.valueOf(assessment.getOrDefault("assessmentType", "UNKNOWN")),
                            String.valueOf(assessment.getOrDefault("feedback", "")),
                            score < 6
                                    ? "Review the fundamentals and practice " + skill + " with a small hands-on task."
                                    : "Maintain this skill with a deeper practical exercise."
                    );
                })
                .toList();
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
